package com.pedidos360.pedidos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DetallePedidoDTO(
        @NotBlank String descripcion,
        @Positive int cantidad,
        @NotNull @Positive BigDecimal valorDeclarado
) {
}
