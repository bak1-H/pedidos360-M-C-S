package com.pedidos360.notificaciones.controller;

import com.pedidos360.notificaciones.dto.CategoriaCola;
import com.pedidos360.notificaciones.dto.ColaResponse;
import com.pedidos360.notificaciones.dto.NotificacionResponse;
import com.pedidos360.notificaciones.dto.PaginaResponse;
import com.pedidos360.notificaciones.dto.ResumenResponse;
import com.pedidos360.notificaciones.exception.SolicitudInvalidaException;
import com.pedidos360.notificaciones.model.EstadoNotificacion;
import com.pedidos360.notificaciones.model.TipoNotificacion;
import com.pedidos360.notificaciones.service.ColaService;
import com.pedidos360.notificaciones.service.NotificacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificacionInternalController.class)
class NotificacionInternalControllerTest {

    private static final String TOKEN = "test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificacionService notificacionService;

    @MockitoBean
    private ColaService colaService;

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/internal/notificaciones"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenIncorrectoDevuelve401() throws Exception {
        mockMvc.perform(get("/internal/notificaciones").header("X-Internal-Token", "otro"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laVarianteConCaracteresCodificadosSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/%69nternal/notificaciones"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenValidoDevuelveLaPaginaConElFormatoPropio() throws Exception {
        NotificacionResponse fila = new NotificacionResponse(UUID.randomUUID(), TipoNotificacion.PEDIDO_CREADO,
                EstadoNotificacion.PROCESADA, "pedido.creado", "notificaciones.pedido.creado", "cliente-1",
                "Tu pedido fue creado", UUID.randomUUID(), Instant.parse("2026-10-10T10:00:00Z"));
        when(notificacionService.listar(eq(TipoNotificacion.PEDIDO_CREADO), eq(null), eq(0), eq(20)))
                .thenReturn(new PaginaResponse<>(List.of(fila), 0, 20, 1, 1));

        mockMvc.perform(get("/internal/notificaciones")
                        .param("tipo", "PEDIDO_CREADO")
                        .header("X-Internal-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].tipo").value("PEDIDO_CREADO"))
                .andExpect(jsonPath("$.contenido[0].estado").value("PROCESADA"))
                .andExpect(jsonPath("$.contenido[0].routingKey").value("pedido.creado"))
                .andExpect(jsonPath("$.contenido[0].cola").value("notificaciones.pedido.creado"))
                .andExpect(jsonPath("$.contenido[0].destinatario").value("cliente-1"))
                .andExpect(jsonPath("$.contenido[0].fechaRecepcion").value("2026-10-10T10:00:00Z"))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(20))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1));
    }

    @Test
    void unTipoInvalidoDevuelve400() throws Exception {
        mockMvc.perform(get("/internal/notificaciones")
                        .param("tipo", "INEXISTENTE")
                        .header("X-Internal-Token", TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unTamanoInvalidoDevuelve400() throws Exception {
        when(notificacionService.listar(any(), any(), eq(0), eq(500)))
                .thenThrow(new SolicitudInvalidaException("tamaño debe estar entre 1 y 100"));

        mockMvc.perform(get("/internal/notificaciones")
                        .param("tamano", "500")
                        .header("X-Internal-Token", TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("tamaño debe estar entre 1 y 100"));
    }

    @Test
    void elResumenDevuelveTotalYContadores() throws Exception {
        Map<String, Long> porTipo = new LinkedHashMap<>();
        porTipo.put("PEDIDO_CREADO", 3L);
        Map<String, Long> porEstado = new LinkedHashMap<>();
        porEstado.put("PROCESADA", 2L);
        porEstado.put("MUERTA", 1L);
        when(notificacionService.resumir()).thenReturn(new ResumenResponse(3, porTipo, porEstado));

        mockMvc.perform(get("/internal/notificaciones/resumen").header("X-Internal-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.porTipo.PEDIDO_CREADO").value(3))
                .andExpect(jsonPath("$.porEstado.MUERTA").value(1));
    }

    @Test
    void lasColasDevuelvenNombreCategoriaMensajesYConsumidores() throws Exception {
        when(colaService.listar()).thenReturn(List.of(
                new ColaResponse("notificaciones.pedido.creado", CategoriaCola.NEGOCIO, 0, 1),
                new ColaResponse("notificaciones.pedido.creado.dlq", CategoriaCola.DLQ, 2, 1)));

        mockMvc.perform(get("/internal/notificaciones/colas").header("X-Internal-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("notificaciones.pedido.creado"))
                .andExpect(jsonPath("$[0].categoria").value("NEGOCIO"))
                .andExpect(jsonPath("$[1].categoria").value("DLQ"))
                .andExpect(jsonPath("$[1].mensajes").value(2))
                .andExpect(jsonPath("$[1].consumidores").value(1));
    }
}
