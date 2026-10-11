package com.pedidos360.bff.controller;

import com.pedidos360.bff.client.NotificacionesClient;
import com.pedidos360.bff.exception.NotificacionesRechazoException;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NotificacionesBffControllerTest {

    private static final String AUTORIZACION = "Bearer token-falso";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private NotificacionesClient notificacionesClient;

    private Jwt tokenConRoles(List<String> roles) {
        return Jwt.withTokenValue("token-falso")
                .header("alg", "RS256")
                .subject("oid-123")
                .claim("oid", "oid-123")
                .claim("roles", roles)
                .audience(List.of("api://pedidos360-bff-test"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    @DisplayName("GET /bff/notificaciones sin token -> 401")
    void anonimoRecibe401() throws Exception {
        mockMvc.perform(get("/bff/notificaciones"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /bff/notificaciones con rol CLIENTE -> 403 en las tres rutas")
    void clienteRecibe403() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(tokenConRoles(List.of("CLIENTE")));

        mockMvc.perform(get("/bff/notificaciones").header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/bff/notificaciones/resumen").header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/bff/notificaciones/colas").header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /bff/notificaciones con rol ADMIN -> 200 y pasa los parametros de consulta")
    void adminListaConParametros() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(tokenConRoles(List.of("ADMIN")));
        when(notificacionesClient.listar(eq("PEDIDO_CREADO"), eq("MUERTA"), eq("1"), eq("5")))
                .thenReturn(Map.of("pagina", 1, "totalElementos", 11));

        mockMvc.perform(get("/bff/notificaciones")
                        .param("tipo", "PEDIDO_CREADO")
                        .param("estado", "MUERTA")
                        .param("pagina", "1")
                        .param("tamano", "5")
                        .header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(11));
    }

    @Test
    @DisplayName("GET /bff/notificaciones/resumen y /colas con rol ADMIN -> 200")
    void adminConsultaResumenYColas() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(tokenConRoles(List.of("ADMIN")));
        when(notificacionesClient.resumir()).thenReturn(Map.of("total", 3));
        when(notificacionesClient.listarColas()).thenReturn(List.of(Map.of("nombre", "notificaciones.pedido.creado")));

        mockMvc.perform(get("/bff/notificaciones/resumen").header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3));
        mockMvc.perform(get("/bff/notificaciones/colas").header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("notificaciones.pedido.creado"));
    }

    @Test
    @DisplayName("un 400 del servicio de notificaciones se propaga con su mensaje")
    void propagaElRechazoDeParametros() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(tokenConRoles(List.of("ADMIN")));
        when(notificacionesClient.listar(any(), any(), any(), any()))
                .thenThrow(new NotificacionesRechazoException(400, "tamaño debe estar entre 1 y 100"));

        mockMvc.perform(get("/bff/notificaciones")
                        .param("tamano", "500")
                        .header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("tamaño debe estar entre 1 y 100"));
    }

    @Test
    @DisplayName("un 401 del servicio interno de notificaciones se informa como 502")
    void tokenInternoRechazadoSeInformaComo502() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(tokenConRoles(List.of("ADMIN")));
        when(notificacionesClient.resumir()).thenThrow(
                WebClientResponseException.create(401, "Unauthorized", HttpHeaders.EMPTY, new byte[0], null));

        mockMvc.perform(get("/bff/notificaciones/resumen").header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isBadGateway());
    }
}
