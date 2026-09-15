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

/** Valida firma, vigencia, issuer y audiencia del token de Azure AD. */
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
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/bff/me").authenticated()

                        // /mios debe ir antes que /{id} o lo captura el comodin.
                        .requestMatchers(HttpMethod.GET, "/bff/pedidos/mios").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.POST, "/bff/pedidos").hasRole("CLIENTE")
                        .requestMatchers(HttpMethod.GET, "/bff/pedidos").hasRole("ADMIN")
                        // Dueño o ADMIN se verifica en el controller, no aca.
                        .requestMatchers(HttpMethod.GET, "/bff/pedidos/*").authenticated()

                        .requestMatchers(HttpMethod.GET, "/bff/envios/mios").hasRole("REPARTIDOR")
                        .requestMatchers(HttpMethod.POST, "/bff/envios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/bff/envios/*/estado")
                                .hasAnyRole("REPARTIDOR", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/bff/usuarios").hasRole("ADMIN")

                        .requestMatchers("/bff/**").authenticated()
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

    /** Pide el JWKS de forma perezosa: la app arranca aunque Azure no responda. */
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
