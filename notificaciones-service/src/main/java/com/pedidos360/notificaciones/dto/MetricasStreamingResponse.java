package com.pedidos360.notificaciones.dto;

import java.util.List;
import java.util.Map;

public record MetricasStreamingResponse(
        long totalEventosPedido,
        long totalEventosEnvio,
        Map<String, Long> pedidosPorEstado,
        Map<String, Long> pedidosPorTipo,
        Map<String, Long> enviosPorEstado,
        List<EventoRecienteResponse> ultimosEventos
) {
}
