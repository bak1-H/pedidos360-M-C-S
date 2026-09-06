package com.pedidos360.bff.security;

import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;

import java.util.List;

/**
 * Valida el claim "aud": el token tiene que haber sido emitido PARA esta API.
 *
 * Por que importa (SDD 7.3): sin esto, cualquier token firmado por el mismo tenant
 * de Azure AD -- incluso uno emitido para otra aplicacion distinta -- pasaria la
 * validacion de firma e issuer y seria aceptado. Es la diferencia entre el 100% y
 * el 60-80% en el indicador del 40%.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final JwtClaimValidator<List<String>> delegado;

    public AudienceValidator(String audienciaEsperada) {
        this.delegado = new JwtClaimValidator<>(
                JwtClaimNames.AUD,
                audiencias -> audiencias != null && audiencias.contains(audienciaEsperada));
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        return delegado.validate(token);
    }
}
