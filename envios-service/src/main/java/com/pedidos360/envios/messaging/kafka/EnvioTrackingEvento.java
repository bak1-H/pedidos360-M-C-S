package com.pedidos360.envios.messaging.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record EnvioTrackingEvento(
        String eventoId,
        UUID envioId,
        UUID pedidoId,
        UUID repartidorId,
        String estado,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant ocurridoEn,
        String descripcion,
        @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDateTime fechaAsignacion,
        @JsonFormat(shape = JsonFormat.Shape.STRING) LocalDateTime fechaEntregaEstimada
) {
}
