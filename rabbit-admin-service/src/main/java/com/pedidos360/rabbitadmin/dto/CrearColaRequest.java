package com.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CrearColaRequest(
        @NotBlank(message = "nombre es obligatorio")
        @Pattern(regexp = PatronesRabbit.NOMBRE, message = "nombre solo admite letras, numeros, punto, guion y guion bajo (maximo 255)")
        String nombre,
        Boolean durable,
        @Pattern(regexp = PatronesRabbit.NOMBRE, message = "deadLetterExchange solo admite letras, numeros, punto, guion y guion bajo (maximo 255)")
        String deadLetterExchange,
        String deadLetterRoutingKey
) {
}
