package com.pedidos360.pedidos.dto;

import com.pedidos360.pedidos.model.TipoPaquete;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaqueteDTO(
        @NotNull @Positive BigDecimal pesoKg,
        @NotNull @Positive BigDecimal alturaCm,
        @NotNull @Positive BigDecimal anchoCm,
        @NotNull @Positive BigDecimal largoCm,
        @NotNull TipoPaquete tipo
) {
}
