package com.pedidos360.bff.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test del indicador que mas se pierde: si solo validas la firma, un token
 * emitido por Azure AD para OTRA aplicacion del mismo tenant entra igual.
 */
class AudienceValidatorTest {

    private static final String AUDIENCIA_ESPERADA = "api://pedidos360-bff";

    private final AudienceValidator validator = new AudienceValidator(AUDIENCIA_ESPERADA);

    private Jwt tokenConAudiencia(List<String> audiencias) {
        return Jwt.withTokenValue("token-falso")
                .header("alg", "RS256")
                .subject("oid-123")
                .audience(audiencias)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    @DisplayName("acepta el token cuando el claim aud contiene nuestra API")
    void aceptaAudienciaCorrecta() {
        OAuth2TokenValidatorResult resultado = validator.validate(
                tokenConAudiencia(List.of(AUDIENCIA_ESPERADA)));

        assertThat(resultado.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("rechaza un token valido de Azure AD emitido para otra aplicacion")
    void rechazaAudienciaDeOtraApp() {
        OAuth2TokenValidatorResult resultado = validator.validate(
                tokenConAudiencia(List.of("api://otra-aplicacion-distinta")));

        assertThat(resultado.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("rechaza el token si no trae claim aud")
    void rechazaSinAudiencia() {
        Jwt sinAud = Jwt.withTokenValue("token-falso")
                .header("alg", "RS256")
                .subject("oid-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        assertThat(validator.validate(sinAud).hasErrors()).isTrue();
    }
}
