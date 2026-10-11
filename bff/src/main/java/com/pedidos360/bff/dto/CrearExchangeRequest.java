package com.pedidos360.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CrearExchangeRequest(
        @NotBlank @Pattern(regexp = PatronesRabbit.NOMBRE) String nombre,
        @NotBlank @Pattern(regexp = PatronesRabbit.TIPO_EXCHANGE) String tipo,
        Boolean durable) {
}
