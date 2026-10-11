package com.pedidos360.pedidos.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EnvioEstadoCambiadoEvent(
        UUID envioId,
        @NotNull UUID pedidoId,
        @NotBlank String estadoEnvio
) {
}
