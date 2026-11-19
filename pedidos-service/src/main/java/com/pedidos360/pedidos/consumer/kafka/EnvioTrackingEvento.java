package com.pedidos360.pedidos.consumer.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record EnvioTrackingEvento(
        @NotBlank String eventoId,
        @NotNull UUID envioId,
        @NotNull UUID pedidoId,
        UUID repartidorId,
        @NotBlank String estado,
        @NotNull @JsonFormat(shape = JsonFormat.Shape.STRING) Instant ocurridoEn
) {
}
