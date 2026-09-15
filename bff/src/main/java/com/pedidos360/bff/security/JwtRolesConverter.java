package com.pedidos360.bff.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;

/** Convierte el claim "roles" (no "scp") en authorities con prefijo ROLE_. */
public class JwtRolesConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String CLAIM_ROLES = "roles";
    private static final String PREFIJO_ROLE = "ROLE_";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(CLAIM_ROLES);

        Collection<GrantedAuthority> authorities = roles == null
                ? List.of()
                : roles.stream()
                        .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority(PREFIJO_ROLE + rol))
                        .toList();

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}
