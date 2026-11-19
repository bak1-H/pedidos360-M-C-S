package com.pedidos360.notificaciones.dto;

import java.time.Instant;
import java.util.UUID;

public record EventoRecienteResponse(
        String origen,
        String eventoId,
        String tipo,
        UUID pedidoId,
        String estado,
        Instant ocurridoEn
) {
}
