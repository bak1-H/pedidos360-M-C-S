package com.pedidos360.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record DetallePedidoDto(
        @NotBlank String descripcion,
        @Positive int cantidad,
        @NotNull @Positive BigDecimal valorDeclarado) {
}
