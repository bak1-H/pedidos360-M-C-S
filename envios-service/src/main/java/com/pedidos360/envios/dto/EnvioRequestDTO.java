package com.pedidos360.envios.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record EnvioRequestDTO(
        @NotNull UUID pedidoId,
        @NotNull UUID repartidorId,
        LocalDateTime fechaEntregaEstimada
) {
}
