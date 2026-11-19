package com.pedidos360.kafkaadmin.dto;

import jakarta.validation.constraints.Min;

public record AumentarParticionesRequest(
        @Min(value = 1, message = "total debe ser al menos 1")
        int total
) {
}
