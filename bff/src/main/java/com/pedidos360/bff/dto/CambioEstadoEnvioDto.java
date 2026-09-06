package com.pedidos360.bff.dto;

import jakarta.validation.constraints.NotBlank;

/** PATCH /bff/envios/{id}/estado y lo que el BFF reenvia a envios-service. */
public record CambioEstadoEnvioDto(
        @NotBlank String estadoEnvio,
        @NotBlank String descripcionEvento) {
}
