package com.pedidos360.envios.dto;

import com.pedidos360.envios.model.EstadoEnvio;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoEnvioDTO(
        @NotNull EstadoEnvio estadoEnvio,
        @NotBlank String descripcionEvento
) {
}
