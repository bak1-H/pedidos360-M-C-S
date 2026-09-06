package com.pedidos360.bff.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Lo que manda Angular a POST /bff/pedidos. Sin clienteId: eso lo resuelve el BFF desde el token. */
public record CrearPedidoRequest(
        @NotBlank String direccionOrigen,
        @NotBlank String direccionDestino,
        @NotEmpty List<@Valid DetallePedidoDto> detalles,
        @NotNull @Valid PaqueteDto paquete) {
}
