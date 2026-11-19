package com.pedidos360.pedidos.messaging.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.util.UUID;

public record PedidoEventoKafka(
        String eventoId,
        String tipo,
        UUID pedidoId,
        UUID clienteId,
        String estado,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant ocurridoEn
) {
}
