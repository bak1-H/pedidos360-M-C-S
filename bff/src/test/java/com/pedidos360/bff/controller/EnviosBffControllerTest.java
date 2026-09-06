package com.pedidos360.bff.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.bff.client.EnviosClient;
import com.pedidos360.bff.client.UsuariosClient;
import com.pedidos360.bff.dto.CambioEstadoEnvioDto;
import com.pedidos360.bff.dto.EnvioResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EnviosBffControllerTest {

    private static final String AUTORIZACION = "Bearer token-falso";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private EnviosClient enviosClient;

    @MockitoBean
    private UsuariosClient usuariosClient;

    private Jwt tokenDe(String oid, List<String> roles) {
        return Jwt.withTokenValue("token-falso")
                .header("alg", "RS256")
                .subject(oid)
                .claim("oid", oid)
                .claim("roles", roles)
                .audience(List.of("api://pedidos360-bff-test"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    @DisplayName("PATCH /bff/envios/{id}/estado el repartidor dueño puede cambiar el estado")
    void repartidorDuenoActualizaEstado() throws Exception {
        UUID repartidorId = UUID.randomUUID();
        UUID envioId = UUID.randomUUID();
        when(jwtDecoder.decode(anyString())).thenReturn(tokenDe("oid-repartidor", List.of("REPARTIDOR")));
        when(usuariosClient.resolverUsuarioInternoId(any())).thenReturn(repartidorId);
        when(enviosClient.buscarPorId(any(), any())).thenReturn(Optional.of(
                new EnvioResponseDto(envioId, UUID.randomUUID(), repartidorId, "PENDIENTE", null, null)));
        when(enviosClient.cambiarEstado(any(), any(), any())).thenReturn(
                new EnvioResponseDto(envioId, UUID.randomUUID(), repartidorId, "EN_TRANSITO", null, null));

        mockMvc.perform(patch("/bff/envios/{id}/estado", envioId)
                        .header(HttpHeaders.AUTHORIZATION, AUTORIZACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CambioEstadoEnvioDto("EN_TRANSITO", "Salio a reparto"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /bff/envios/{id}/estado un repartidor que no es el dueño recibe 403")
    void repartidorNoDuenoRecibe403() throws Exception {
        UUID envioId = UUID.randomUUID();
        when(jwtDecoder.decode(anyString())).thenReturn(tokenDe("oid-otro-repartidor", List.of("REPARTIDOR")));
        when(usuariosClient.resolverUsuarioInternoId(any())).thenReturn(UUID.randomUUID());
        when(enviosClient.buscarPorId(any(), any())).thenReturn(Optional.of(
                new EnvioResponseDto(envioId, UUID.randomUUID(), UUID.randomUUID(), "PENDIENTE", null, null)));

        mockMvc.perform(patch("/bff/envios/{id}/estado", envioId)
                        .header(HttpHeaders.AUTHORIZATION, AUTORIZACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CambioEstadoEnvioDto("EN_TRANSITO", "Salio a reparto"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PATCH /bff/envios/{id}/estado el ADMIN puede cambiar cualquier envio sin chequeo de dueño")
    void adminActualizaCualquierEnvio() throws Exception {
        UUID envioId = UUID.randomUUID();
        when(jwtDecoder.decode(anyString())).thenReturn(tokenDe("oid-admin", List.of("ADMIN")));
        when(enviosClient.cambiarEstado(any(), any(), any())).thenReturn(
                new EnvioResponseDto(envioId, UUID.randomUUID(), UUID.randomUUID(), "FALLIDO", null, null));

        mockMvc.perform(patch("/bff/envios/{id}/estado", envioId)
                        .header(HttpHeaders.AUTHORIZATION, AUTORIZACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CambioEstadoEnvioDto("FALLIDO", "No se pudo entregar"))))
                .andExpect(status().isOk());
    }
}
