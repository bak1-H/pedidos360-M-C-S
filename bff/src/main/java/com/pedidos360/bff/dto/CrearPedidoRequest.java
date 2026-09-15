package com.pedidos360.bff.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CrearPedidoRequest(
        @NotBlank String direccionOrigen,
        @NotBlank String direccionDestino,
        @NotEmpty List<@Valid DetallePedidoDto> detalles,
        @NotNull @Valid PaqueteDto paquete) {
}
