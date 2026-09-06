package com.pedidos360.bff.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/** POST /bff/envios (ADMIN) y lo que el BFF reenvia a POST /internal/envios. */
public record EnvioRequestDto(
        @NotNull UUID pedidoId,
        @NotNull UUID repartidorId,
        LocalDateTime fechaEntregaEstimada) {
}
