package com.pedidos360.bff.dto;

import jakarta.validation.constraints.NotBlank;

public record CambioEstadoEnvioDto(
        @NotBlank String estadoEnvio,
        @NotBlank String descripcionEvento) {
}
