package com.pedidos360.kafkaadmin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

public record CrearTopicoRequest(
        @NotBlank(message = "nombre es obligatorio")
        @Pattern(regexp = PatronesKafka.NOMBRE, message = "nombre solo admite letras, números, punto, guion y guion bajo (máximo 249)")
        String nombre,
        @Min(value = 1, message = "particiones debe ser al menos 1")
        Integer particiones,
        @Min(value = 1, message = "replicas debe ser al menos 1")
        Short replicas,
        Map<String, String> configuracion
) {
}
