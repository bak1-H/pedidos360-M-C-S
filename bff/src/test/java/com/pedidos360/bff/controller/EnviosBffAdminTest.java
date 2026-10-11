package com.pedidos360.bff.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.bff.client.EnviosClient;
import com.pedidos360.bff.client.PedidosClient;
import com.pedidos360.bff.client.UsuariosClient;
import com.pedidos360.bff.dto.CambioEstadoEnvioDto;
import com.pedidos360.bff.dto.EnvioRequestDto;
import com.pedidos360.bff.dto.EnvioResponseDto;
import com.pedidos360.bff.dto.PedidoResponseDto;
import com.pedidos360.bff.exception.EnvioRechazoException;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EnviosBffAdminTest {

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
    private PedidosClient pedidosClient;

    @MockitoBean
    private UsuariosClient usuariosClient;

    private void autenticarComo(String rol) {
        when(jwtDecoder.decode(anyString())).thenReturn(Jwt.withTokenValue("token-falso")
                .header("alg", "RS256")
                .subject("oid-1")
                .claim("oid", "oid-1")
                .claim("roles", List.of(rol))
                .audience(List.of("api://pedidos360-bff-test"))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build());
    }

    private PedidoResponseDto pedidoEn(String estado) {
        return new PedidoResponseDto(UUID.randomUUID(), UUID.randomUUID(), estado, "origen", "destino", null, null, null);
    }

    private String cuerpoCrear(UUID pedidoId) throws Exception {
        return objectMapper.writeValueAsString(new EnvioRequestDto(pedidoId, UUID.randomUUID(), null));
    }

    @Test
    @DisplayName("GET /bff/envios anonimo -> 401")
    void anonimoRecibe401() throws Exception {
        mockMvc.perform(get("/bff/envios")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /bff/envios CLIENTE y REPARTIDOR -> 403")
    void clienteYRepartidorRecibenNoAutorizado() throws Exception {
        autenticarComo("CLIENTE");
        mockMvc.perform(get("/bff/envios").header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isForbidden());

        autenticarComo("REPARTIDOR");
        mockMvc.perform(get("/bff/envios").header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /bff/envios ADMIN -> 200 con la lista de envios")
    void adminListaTodosLosEnvios() throws Exception {
        autenticarComo("ADMIN");
        UUID id = UUID.randomUUID();
        when(enviosClient.listarTodos(any())).thenReturn(List.of(
                new EnvioResponseDto(id, UUID.randomUUID(), UUID.randomUUID(), "PENDIENTE", null, null)));

        mockMvc.perform(get("/bff/envios").header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].estadoEnvio").value("PENDIENTE"));
    }

    @Test
    @DisplayName("POST /bff/envios con pedido CREADO -> 201")
    void creaElEnvioSiElPedidoEstaCreado() throws Exception {
        autenticarComo("ADMIN");
        PedidoResponseDto pedido = pedidoEn("CREADO");
        when(pedidosClient.buscarPorId(any(), any())).thenReturn(Optional.of(pedido));
        when(enviosClient.crear(any(), any())).thenReturn(
                new EnvioResponseDto(UUID.randomUUID(), pedido.id(), UUID.randomUUID(), "PENDIENTE", null, null));

        mockMvc.perform(post("/bff/envios")
                        .header(HttpHeaders.AUTHORIZATION, AUTORIZACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCrear(pedido.id())))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /bff/envios con pedido despachado, cancelado o inexistente -> 409 PEDIDO_NO_ASIGNABLE")
    void rechazaUnPedidoQueNoEstaCreado() throws Exception {
        autenticarComo("ADMIN");
        for (String estado : List.of("DESPACHADO", "CANCELADO", "ENTREGADO")) {
            when(pedidosClient.buscarPorId(any(), any())).thenReturn(Optional.of(pedidoEn(estado)));
            mockMvc.perform(post("/bff/envios")
                            .header(HttpHeaders.AUTHORIZATION, AUTORIZACION)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoCrear(UUID.randomUUID())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.codigo").value("PEDIDO_NO_ASIGNABLE"));
        }
        when(pedidosClient.buscarPorId(any(), any())).thenReturn(Optional.empty());
        mockMvc.perform(post("/bff/envios")
                        .header(HttpHeaders.AUTHORIZATION, AUTORIZACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCrear(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PEDIDO_NO_ASIGNABLE"));

        verify(enviosClient, never()).crear(any(), any());
    }

    @Test
    @DisplayName("POST /bff/envios propaga el 409 ENVIO_DUPLICADO del servicio de envios")
    void propagaEnvioDuplicado() throws Exception {
        autenticarComo("ADMIN");
        when(pedidosClient.buscarPorId(any(), any())).thenReturn(Optional.of(pedidoEn("CREADO")));
        when(enviosClient.crear(any(), any())).thenThrow(
                new EnvioRechazoException(409, "ENVIO_DUPLICADO", "Ya existe un envío para el pedido"));

        mockMvc.perform(post("/bff/envios")
                        .header(HttpHeaders.AUTHORIZATION, AUTORIZACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoCrear(UUID.randomUUID())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("ENVIO_DUPLICADO"))
                .andExpect(jsonPath("$.mensaje").value("Ya existe un envío para el pedido"));
    }

    @Test
    @DisplayName("PATCH /bff/envios/{id}/estado propaga el 409 TRANSICION_INVALIDA")
    void propagaTransicionInvalida() throws Exception {
        autenticarComo("ADMIN");
        when(enviosClient.cambiarEstado(any(), any(), any())).thenThrow(
                new EnvioRechazoException(409, "TRANSICION_INVALIDA", "No se puede cambiar un envío de ENTREGADO a PENDIENTE"));

        mockMvc.perform(patch("/bff/envios/{id}/estado", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, AUTORIZACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CambioEstadoEnvioDto("PENDIENTE", "reintento"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
    }

    @Test
    @DisplayName("un 401 del servicio de envios se informa como 502")
    void tokenInternoRechazadoSeInformaComo502() throws Exception {
        autenticarComo("ADMIN");
        when(enviosClient.listarTodos(any())).thenThrow(
                WebClientResponseException.create(401, "Unauthorized", HttpHeaders.EMPTY, new byte[0], null));

        mockMvc.perform(get("/bff/envios").header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isBadGateway());
    }
}
