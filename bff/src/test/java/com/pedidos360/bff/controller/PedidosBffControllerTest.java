package com.pedidos360.bff.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.bff.client.EnviosClient;
import com.pedidos360.bff.client.PedidosClient;
import com.pedidos360.bff.client.UsuariosClient;
import com.pedidos360.bff.dto.CrearPedidoRequest;
import com.pedidos360.bff.dto.DetallePedidoDto;
import com.pedidos360.bff.dto.PaqueteDto;
import com.pedidos360.bff.dto.PedidoResponseDto;
import com.pedidos360.bff.dto.UsuarioDto;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PedidosBffControllerTest {

    private static final String AUTORIZACION = "Bearer token-falso";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private PedidosClient pedidosClient;

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

    private CrearPedidoRequest pedidoValido() {
        return new CrearPedidoRequest(
                "Origen 123",
                "Destino 456",
                List.of(new DetallePedidoDto("Item", 1, BigDecimal.TEN)),
                new PaqueteDto(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, "CAJA"));
    }

    @Test
    @DisplayName("POST /bff/pedidos como CLIENTE crea el pedido con el clienteId resuelto del token")
    void clienteCreaPedido() throws Exception {
        UUID clienteId = UUID.randomUUID();
        when(jwtDecoder.decode(anyString())).thenReturn(tokenDe("oid-cliente", List.of("CLIENTE")));
        when(usuariosClient.resolverUsuarioInternoId(any())).thenReturn(clienteId);
        when(pedidosClient.crear(any(), any())).thenReturn(new PedidoResponseDto(
                UUID.randomUUID(), clienteId, "CREADO", "Origen 123", "Destino 456", null, List.of(), null));

        mockMvc.perform(post("/bff/pedidos")
                        .header(HttpHeaders.AUTHORIZATION, AUTORIZACION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pedidoValido())))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET /bff/pedidos/{id} el dueño del pedido puede verlo")
    void duenoVeSuPedido() throws Exception {
        UUID clienteId = UUID.randomUUID();
        UUID pedidoId = UUID.randomUUID();
        when(jwtDecoder.decode(anyString())).thenReturn(tokenDe("oid-cliente", List.of("CLIENTE")));
        when(usuariosClient.resolverUsuarioInternoId(any())).thenReturn(clienteId);
        when(pedidosClient.buscarPorId(any(), any())).thenReturn(Optional.of(new PedidoResponseDto(
                pedidoId, clienteId, "CREADO", "Origen", "Destino", null, List.of(), null)));
        when(enviosClient.buscarPorPedido(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/bff/pedidos/{id}", pedidoId).header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /bff/pedidos/{id} un CLIENTE que no es el dueño recibe 403")
    void noDuenoRecibe403() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        when(jwtDecoder.decode(anyString())).thenReturn(tokenDe("oid-otro-cliente", List.of("CLIENTE")));
        when(usuariosClient.resolverUsuarioInternoId(any())).thenReturn(UUID.randomUUID());
        when(pedidosClient.buscarPorId(any(), any())).thenReturn(Optional.of(new PedidoResponseDto(
                pedidoId, UUID.randomUUID(), "CREADO", "Origen", "Destino", null, List.of(), null)));

        mockMvc.perform(get("/bff/pedidos/{id}", pedidoId).header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /bff/pedidos/{id} el ADMIN puede ver cualquier pedido sin chequeo de dueño")
    void adminVeCualquierPedido() throws Exception {
        UUID pedidoId = UUID.randomUUID();
        when(jwtDecoder.decode(anyString())).thenReturn(tokenDe("oid-admin", List.of("ADMIN")));
        when(pedidosClient.buscarPorId(any(), any())).thenReturn(Optional.of(new PedidoResponseDto(
                pedidoId, UUID.randomUUID(), "CREADO", "Origen", "Destino", null, List.of(), null)));
        when(enviosClient.buscarPorPedido(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/bff/pedidos/{id}", pedidoId).header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /bff/pedidos/{id} pedido inexistente -> 404")
    void pedidoInexistenteDevuelve404() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(tokenDe("oid-admin", List.of("ADMIN")));
        when(pedidosClient.buscarPorId(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/bff/pedidos/{id}", UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, AUTORIZACION))
                .andExpect(status().isNotFound());
    }
}
