package com.pedidos360.bff.security;

import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;

import java.util.List;

/** Exige que el claim "aud" sea esta API: un token de otra app del mismo tenant no entra. */
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
