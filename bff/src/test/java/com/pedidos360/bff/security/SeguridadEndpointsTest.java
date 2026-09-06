package com.pedidos360.bff.security;

import com.pedidos360.bff.client.UsuariosClient;
import com.pedidos360.bff.dto.UsuarioDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los tests que pide el SDD 7.2, uno por cada forma de fallar:
 *   - sin token                  -> 401
 *   - token mal firmado          -> 401
 *   - token expirado             -> 401
 *   - token valido, rol que no   -> 403
 *   - token valido, rol correcto -> 200
 *
 * Reemplazamos el JwtDecoder por un mock para no depender de Azure AD en el build.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadEndpointsTest {

    private static final String AUTORIZACION = "Bearer token-falso";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UsuariosClient usuariosClient;

    private Jwt tokenConRoles(List<String> roles) {
        return Jwt.withTokenValue("token-falso")
                .header("alg", "RS256")
                .subject("oid-123")
                .claim("oid", "oid-123")
                .claim("name", "Ana Soto")
                .claim("preferred_username", "ana@pedidos360.cl")
                .claim("roles", roles)
                .audience(List.of("api://pedidos360-bff-test"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    @DisplayName("sin token -> 401 con JSON, no HTML")
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/bff/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("token con firma invalida -> 401")
    void tokenMalFirmadoDevuelve401() throws Exception {
        when(jwtDecoder.decode(anyString()))
                .thenThrow(new BadJwtException("La firma del token no es valida"));

        mockMvc.perform(get("/bff/usuarios").header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("token expirado -> 401")
    void tokenExpiradoDevuelve401() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new JwtValidationException(
                "El token expiro",
                List.of(new OAuth2Error("invalid_token", "Jwt expired", null))));

        mockMvc.perform(get("/bff/usuarios").header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("token valido pero rol CLIENTE en un endpoint de ADMIN -> 403, no 401")
    void rolIncorrectoDevuelve403() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(tokenConRoles(List.of("CLIENTE")));

        mockMvc.perform(get("/bff/usuarios").header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
    }

    @Test
    @DisplayName("token valido con rol ADMIN -> 200")
    void rolAdminAccede() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(tokenConRoles(List.of("ADMIN")));
        when(usuariosClient.listar(any())).thenReturn(List.of(
                new UsuarioDto(UUID.randomUUID(), "oid-123", "Ana Soto",
                        "ana@pedidos360.cl", "ADMIN", null)));

        mockMvc.perform(get("/bff/usuarios").header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Ana Soto"));
    }

    @Test
    @DisplayName("/bff/me acepta cualquier rol y aprovisiona el perfil si no existia")
    void meAprovisionaPerfilNuevo() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(tokenConRoles(List.of("CLIENTE")));
        when(usuariosClient.buscarPorOid(any())).thenReturn(Optional.empty());
        when(usuariosClient.crear(any(), any())).thenReturn(
                new UsuarioDto(UUID.randomUUID(), "oid-123", "Ana Soto",
                        "ana@pedidos360.cl", "CLIENTE", null));

        mockMvc.perform(get("/bff/me").header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recienCreado").value(true))
                .andExpect(jsonPath("$.roles[0]").value("CLIENTE"));
    }

    @Test
    @DisplayName("/actuator/health queda publico para el gateway")
    void healthEsPublico() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
