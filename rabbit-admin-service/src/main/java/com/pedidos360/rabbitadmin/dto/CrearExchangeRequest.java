package com.pedidos360.rabbitadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CrearExchangeRequest(
        @NotBlank(message = "nombre es obligatorio")
        @Pattern(regexp = PatronesRabbit.NOMBRE, message = "nombre solo admite letras, numeros, punto, guion y guion bajo (maximo 255)")
        String nombre,
        @NotBlank(message = "tipo es obligatorio")
        @Pattern(regexp = PatronesRabbit.TIPO_EXCHANGE, message = "tipo debe ser direct, topic, fanout o headers")
        String tipo,
        Boolean durable
) {
}
