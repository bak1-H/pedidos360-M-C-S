package com.pedidos360.rabbitadmin.dto;

public record ColaResponse(
        String nombre,
        boolean durable,
        String deadLetterExchange,
        String deadLetterRoutingKey
) {
}
