package com.pedidos360.pedidos.consumer.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record PedidoEventoKafka(
        @NotBlank String eventoId,
        @NotBlank String tipo,
        @NotNull UUID pedidoId,
        @NotNull UUID clienteId,
        @NotBlank String estado,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING) Instant ocurridoEn
) {
}
