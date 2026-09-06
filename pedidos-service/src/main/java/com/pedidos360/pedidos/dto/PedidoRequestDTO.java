package com.pedidos360.pedidos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record PedidoRequestDTO(
        @NotNull UUID clienteId,
        @NotBlank String direccionOrigen,
        @NotBlank String direccionDestino,
        @NotEmpty List<@Valid DetallePedidoDTO> detalles,
        @NotNull @Valid PaqueteDTO paquete
) {
}
