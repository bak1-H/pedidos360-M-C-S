package com.pedidos360.kafkaadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetOffsetsRequest(
        @NotBlank(message = "destino es obligatorio")
        @Pattern(regexp = PatronesKafka.DESTINO_OFFSETS, message = "destino debe ser earliest o latest")
        String destino,
        String topico
) {
}
