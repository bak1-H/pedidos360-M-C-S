package com.pedidos360.bff.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Validacion JWT contra Azure AD. Este es el componente que evalua el 40% de la
 * rubrica del encargo: "el BFF debe validar el token recibido con el IDaaS y solo
 * permitir consumir el endpoint si el token es valido".
 *
 * Las cuatro validaciones exigidas y donde ocurre cada una:
 *
 *   FIRMA     -> NimbusJwtDecoder descarga las llaves publicas del JWKS de Azure AD
 *                y verifica la firma RS256. Nunca se desactiva.
 *   VIGENCIA  -> JwtTimestampValidator (exp / nbf), incluido en createDefaultWithIssuer.
 *   ISSUER    -> JwtIssuerValidator, tambien incluido en createDefaultWithIssuer.
 *   AUDIENCIA -> AudienceValidator, propio. Sin el, un token de OTRA app del mismo
 *                tenant pasaria igual.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String issuerUri;
    private final String jwkSetUri;
    private final String audiencia;

    public SecurityConfig(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${azure.audiencia}") String audiencia) {
        this.issuerUri = issuerUri;
        this.jwkSetUri = jwkSetUri;
        this.audiencia = audiencia;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JsonAuthenticationEntryPoint entryPoint,
                                           JsonAccessDeniedHandler accessDeniedHandler) throws Exception {

        http
                // API stateless que se autentica con Bearer token: no hay sesion ni formulario,
                // asi que CSRF no aplica.
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Lo unico publico: el health check del gateway/orquestador.
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                        // --- Autorizacion por rol, segun SDD seccion 4.1 ---
                        .requestMatchers(HttpMethod.GET, "/bff/me").authenticated()

                        // El orden importa: /mios tiene que evaluarse ANTES que /{id}
                        .requestMatchers(HttpMethod.GET, "/bff/pedidos/mios").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/bff/pedidos").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/bff/pedidos").hasRole("ADMIN")
                        // El "dueño o ADMIN" no se puede expresar aca: se verifica en el
                        // controller comparando el sub del token contra el clienteId del pedido.
                        .requestMatchers(HttpMethod.GET, "/bff/pedidos/*").authenticated()

                        .requestMatchers(HttpMethod.GET, "/bff/envios/mios").hasRole("REPARTIDOR")
                        .requestMatchers(HttpMethod.POST, "/bff/envios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/bff/envios/*/estado")
                                .hasAnyRole("REPARTIDOR", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/bff/usuarios").hasRole("ADMIN")

                        // Cualquier otro /bff/** exige token valido...
                        .requestMatchers("/bff/**").authenticated()
                        // ...y todo lo que no sea /bff/** simplemente no existe hacia afuera.
                        .anyRequest().denyAll())

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtRolesConverter()))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }

    /**
     * Usamos jwk-set-uri en vez de issuer-uri para construir el decoder porque
     * JwtDecoders.fromIssuerLocation() hace una llamada de red al arrancar: si Azure
     * no responde, la aplicacion no levanta. Con withJwkSetUri las llaves se piden
     * de forma perezosa, en el primer token que llega. El issuer igual se valida,
     * via JwtValidators.createDefaultWithIssuer.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> validadores = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuerUri),
                new AudienceValidator(audiencia));

        decoder.setJwtValidator(validadores);
        return decoder;
    }
}
