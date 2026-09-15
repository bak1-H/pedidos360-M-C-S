package com.pedidos360.bff.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/** Identidad ya validada que el BFF propaga por headers a los microservicios. */
public record IdentidadInterna(String oid, List<String> roles) {

    public static final String HEADER_OID = "X-Usuario-Oid";
    public static final String HEADER_ROLES = "X-Usuario-Roles";

    /** Usa "oid" (estable por tenant) y cae a "sub" si no viene. */
    public static IdentidadInterna desde(Jwt jwt) {
        String oid = jwt.getClaimAsString("oid");
        if (oid == null || oid.isBlank()) {
            oid = jwt.getSubject();
        }

        List<String> roles = jwt.getClaimAsStringList(JwtRolesConverter.CLAIM_ROLES);
        return new IdentidadInterna(oid, roles == null ? List.of() : roles);
    }

    public void aplicar(HttpHeaders headers) {
        headers.set(HEADER_OID, oid);
        headers.set(HEADER_ROLES, String.join(",", roles));
    }

    /** Rol para aprovisionar el perfil; CLIENTE si el token no trae ninguno. */
    public String rolPrincipal() {
        return roles.isEmpty() ? "CLIENTE" : roles.get(0);
    }

    /** Para los chequeos de dueño o ADMIN que dependen de un dato de negocio. */
    public boolean esAdmin() {
        return roles.contains("ADMIN");
    }
}
