package com.pedidos360.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaqueteDto(
        @NotNull @Positive BigDecimal pesoKg,
        @NotNull @Positive BigDecimal alturaCm,
        @NotNull @Positive BigDecimal anchoCm,
        @NotNull @Positive BigDecimal largoCm,
        @NotBlank String tipo) {
}
