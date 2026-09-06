package com.pedidos360.bff.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Identidad ya validada que el BFF propaga hacia los microservicios internos
 * (SDD 7.2: "propagar el sub/rol via header interno"). Los microservicios NO
 * vuelven a validar el JWT completo: confian en que el BFF ya lo hizo.
 */
public record IdentidadInterna(String oid, List<String> roles) {

    public static final String HEADER_OID = "X-Usuario-Oid";
    public static final String HEADER_ROLES = "X-Usuario-Roles";

    /**
     * Preferimos el claim "oid" sobre "sub": en Azure AD, "oid" es el Object ID
     * estable del usuario en el tenant, mientras que "sub" es distinto para cada
     * aplicacion. Como la columna en la BD se llama azureAdObjectId, "oid" es el
     * que corresponde. Igual dejamos "sub" de respaldo por si el token no lo trae.
     */
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

    /** Rol principal para aprovisionar el perfil. Si el token no trae ninguno, CLIENTE. */
    public String rolPrincipal() {
        return roles.isEmpty() ? "CLIENTE" : roles.get(0);
    }

    /**
     * Para los chequeos de "dueño o ADMIN" que SecurityConfig no puede expresar
     * con requestMatchers, porque dependen de un dato de negocio (clienteId,
     * repartidorId) y no solo del rol.
     */
    public boolean esAdmin() {
        return roles.contains("ADMIN");
    }
}
