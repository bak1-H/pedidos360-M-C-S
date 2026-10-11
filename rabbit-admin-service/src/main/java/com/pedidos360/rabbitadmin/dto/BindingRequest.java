package com.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BindingRequest(
        @NotBlank(message = "exchange es obligatorio")
        @Pattern(regexp = PatronesRabbit.NOMBRE, message = "exchange solo admite letras, numeros, punto, guion y guion bajo (maximo 255)")
        String exchange,
        @NotBlank(message = "destino es obligatorio")
        @Pattern(regexp = PatronesRabbit.NOMBRE, message = "destino solo admite letras, numeros, punto, guion y guion bajo (maximo 255)")
        String destino,
        @Pattern(regexp = PatronesRabbit.TIPO_DESTINO, message = "tipoDestino debe ser queue o exchange")
        String tipoDestino,
        String routingKey
) {
}
