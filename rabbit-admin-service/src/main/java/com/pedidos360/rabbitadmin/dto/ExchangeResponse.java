package com.pedidos360.rabbitadmin.dto;

public record ExchangeResponse(
        String nombre,
        String tipo,
        boolean durable
) {
}
