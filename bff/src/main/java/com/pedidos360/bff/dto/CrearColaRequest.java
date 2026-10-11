package com.pedidos360.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CrearColaRequest(
        @NotBlank @Pattern(regexp = PatronesRabbit.NOMBRE) String nombre,
        Boolean durable,
        @Pattern(regexp = PatronesRabbit.NOMBRE) String deadLetterExchange,
        String deadLetterRoutingKey) {
}
