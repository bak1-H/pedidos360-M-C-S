package com.pedidos360.bff.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record EnvioRequestDto(
        @NotNull UUID pedidoId,
        @NotNull UUID repartidorId,
        LocalDateTime fechaEntregaEstimada) {
}
