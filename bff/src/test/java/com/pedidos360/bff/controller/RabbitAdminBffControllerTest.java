package com.pedidos360.bff.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.bff.client.RabbitAdminClient;
import com.pedidos360.bff.dto.CrearColaRequest;
import com.pedidos360.bff.exception.RabbitAdminRechazoException;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RabbitAdminBffControllerTest {

    private static final String AUTORIZACION = "Bearer token-falso";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private RabbitAdminClient rabbitAdminClient;

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

    private String cuerpoCola(String nombre) throws Exception {
        return objectMapper.writeValueAsString(new CrearColaRequest(nombre, null, null, null));
    }

    @Test
    @DisplayName("POST /bff/rabbit/queues un CLIENTE recibe 403")
    void clienteNoPuedeAdministrarRabbit() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(tokenConRoles(List.of("CLIENTE")));

        mockMvc.perform(post("/bff/rabbit/queues")
                        .header(HttpHeaders.AUTHORIZATION, AUTORIZACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCola("cola.prueba")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /bff/rabbit/queues el ADMIN crea la cola y recibe 201")
    void adminCreaCola() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(tokenConRoles(List.of("ADMIN")));
        when(rabbitAdminClient.crearCola(any())).thenReturn(Map.of("nombre", "cola.prueba"));

        mockMvc.perform(post("/bff/rabbit/queues")
                        .header(HttpHeaders.AUTHORIZATION, AUTORIZACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCola("cola.prueba")))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("DELETE /bff/rabbit/exchanges/{nombre} propaga el 409 del servicio con su mensaje")
    void propagaConflictoDelServicio() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(tokenConRoles(List.of("ADMIN")));
        doThrow(new RabbitAdminRechazoException(409, "El recurso amq.topic esta protegido"))
                .when(rabbitAdminClient).eliminarExchange("amq.topic");

        mockMvc.perform(delete("/bff/rabbit/exchanges/{nombre}", "amq.topic")
                        .header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El recurso amq.topic esta protegido"));
    }

    @Test
    @DisplayName("POST /bff/rabbit/bindings propaga el 400 del servicio con su mensaje")
    void propagaSolicitudInvalidaDelServicio() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(tokenConRoles(List.of("ADMIN")));
        when(rabbitAdminClient.crearBinding(any()))
                .thenThrow(new RabbitAdminRechazoException(400, "destino es obligatorio"));

        mockMvc.perform(post("/bff/rabbit/bindings")
                        .header(HttpHeaders.AUTHORIZATION, AUTORIZACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exchange\":\"eventos\",\"destino\":\"cola\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("destino es obligatorio"));
    }

    @Test
    @DisplayName("POST /bff/rabbit/queues un 401 del servicio interno se informa como 502")
    void tokenInternoRechazadoSeInformaComo502() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(tokenConRoles(List.of("ADMIN")));
        when(rabbitAdminClient.crearCola(any())).thenThrow(
                WebClientResponseException.create(401, "Unauthorized", HttpHeaders.EMPTY, new byte[0], null));

        mockMvc.perform(post("/bff/rabbit/queues")
                        .header(HttpHeaders.AUTHORIZATION, AUTORIZACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCola("cola.prueba")))
                .andExpect(status().isBadGateway());
    }

    @Test
    @DisplayName("POST /bff/rabbit/queues un 403 del servicio interno se informa como 502")
    void accesoInternoProhibidoSeInformaComo502() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(tokenConRoles(List.of("ADMIN")));
        when(rabbitAdminClient.crearCola(any())).thenThrow(
                WebClientResponseException.create(403, "Forbidden", HttpHeaders.EMPTY, new byte[0], null));

        mockMvc.perform(post("/bff/rabbit/queues")
                        .header(HttpHeaders.AUTHORIZATION, AUTORIZACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCola("cola.prueba")))
                .andExpect(status().isBadGateway());
    }

    @Test
    @DisplayName("POST /bff/rabbit/queues con nombre invalido recibe 400")
    void nombreInvalidoRecibe400() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(tokenConRoles(List.of("ADMIN")));

        mockMvc.perform(post("/bff/rabbit/queues")
                        .header(HttpHeaders.AUTHORIZATION, AUTORIZACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCola("nombre con espacios")))
                .andExpect(status().isBadRequest());
    }
}
