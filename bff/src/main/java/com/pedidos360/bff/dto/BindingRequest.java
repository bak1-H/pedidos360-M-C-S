package com.pedidos360.bff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BindingRequest(
        @NotBlank @Pattern(regexp = PatronesRabbit.NOMBRE) String exchange,
        @NotBlank @Pattern(regexp = PatronesRabbit.NOMBRE) String destino,
        @Pattern(regexp = PatronesRabbit.TIPO_DESTINO) String tipoDestino,
        String routingKey) {
}
