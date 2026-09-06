package com.pedidos360.bff.controller;

import com.pedidos360.bff.client.UsuariosClient;
import com.pedidos360.bff.dto.UsuarioDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MeControllerTest {

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
                .subject("oid-1")
                .claim("oid", "oid-1")
                .claim("name", "Ana Soto")
                .claim("preferred_username", "ana@pedidos360.cl")
                .claim("roles", roles)
                .audience(List.of("api://pedidos360-bff-test"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    private UsuarioDto usuarioConRol(UUID id, String rol) {
        return new UsuarioDto(id, "oid-1", "Ana Soto", "ana@pedidos360.cl", rol, null);
    }

    @Test
    @DisplayName("si el rol de Azure AD cambio, se sincroniza el guardado en usuarios-service")
    void sincronizaRolDesactualizado() throws Exception {
        UUID id = UUID.randomUUID();
        when(jwtDecoder.decode(anyString())).thenReturn(tokenConRoles(List.of("CLIENTE")));
        when(usuariosClient.buscarPorOid(any())).thenReturn(Optional.of(usuarioConRol(id, "ADMIN")));
        when(usuariosClient.actualizarRol(eq(id), eq("CLIENTE"), any()))
                .thenReturn(usuarioConRol(id, "CLIENTE"));

        mockMvc.perform(get("/bff/me").header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("CLIENTE"));

        verify(usuariosClient).actualizarRol(eq(id), eq("CLIENTE"), any());
    }

    @Test
    @DisplayName("si el rol coincide, no se llama a usuarios-service de mas")
    void noSincronizaSiElRolCoincide() throws Exception {
        UUID id = UUID.randomUUID();
        when(jwtDecoder.decode(anyString())).thenReturn(tokenConRoles(List.of("ADMIN")));
        when(usuariosClient.buscarPorOid(any())).thenReturn(Optional.of(usuarioConRol(id, "ADMIN")));

        mockMvc.perform(get("/bff/me").header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isOk());

        verify(usuariosClient, never()).actualizarRol(any(), anyString(), any());
    }

    @Test
    @DisplayName("un token sin roles NO puede borrar el rol guardado")
    void tokenSinRolesNoPisaElRolGuardado() throws Exception {
        UUID id = UUID.randomUUID();
        when(jwtDecoder.decode(anyString())).thenReturn(tokenConRoles(List.of()));
        when(usuariosClient.buscarPorOid(any())).thenReturn(Optional.of(usuarioConRol(id, "ADMIN")));

        mockMvc.perform(get("/bff/me").header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isOk());

        verify(usuariosClient, never()).actualizarRol(any(), anyString(), any());
    }
}
