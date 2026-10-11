package com.pedidos360.notificaciones.event;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record EnvioAsignadoEvent(
        @NotNull UUID envioId,
        @NotNull UUID pedidoId,
        @NotNull UUID repartidorId,
        LocalDateTime fechaEntregaEstimada
) {
}
