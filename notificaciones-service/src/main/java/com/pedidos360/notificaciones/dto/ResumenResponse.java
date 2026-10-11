package com.pedidos360.notificaciones.dto;

import java.util.Map;

public record ResumenResponse(
        long total,
        Map<String, Long> porTipo,
        Map<String, Long> porEstado
) {
}
