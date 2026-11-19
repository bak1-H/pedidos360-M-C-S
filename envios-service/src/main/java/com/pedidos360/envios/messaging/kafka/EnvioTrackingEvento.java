package com.pedidos360.envios.messaging.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.UUID;

public record EnvioTrackingEvento(
        String eventoId,
        UUID envioId,
        UUID pedidoId,
        UUID repartidorId,
        String estado,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant ocurridoEn
) {
}
