package com.pedidos360.bff.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtRolesConverterTest {

    private final JwtRolesConverter converter = new JwtRolesConverter();

    private Jwt.Builder base() {
        return Jwt.withTokenValue("token-falso")
                .header("alg", "RS256")
                .subject("oid-123")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
    }

    @Test
    @DisplayName("convierte el claim roles en authorities con prefijo ROLE_")
    void agregaPrefijoRole() {
        Jwt jwt = base().claim("roles", List.of("ADMIN", "CLIENTE")).build();

        List<String> authorities = converter.convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(authorities).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_CLIENTE");
    }

    @Test
    @DisplayName("un token sin claim roles queda autenticado pero sin ninguna authority")
    void sinRolesQuedaSinAuthorities() {
        Jwt jwt = base().build();

        assertThat(converter.convert(jwt).getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("ignora el claim scp: los scopes no son roles")
    void ignoraScp() {
        Jwt jwt = base().claim("scp", "acceso.total").build();

        assertThat(converter.convert(jwt).getAuthorities()).isEmpty();
    }
}
