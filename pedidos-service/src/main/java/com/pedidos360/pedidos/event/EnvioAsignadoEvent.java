package com.pedidos360.pedidos.event;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EnvioAsignadoEvent(
        UUID envioId,
        @NotNull UUID pedidoId
) {
}
