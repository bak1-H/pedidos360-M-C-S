package com.pedidos360.rabbitadmin.dto;

public record ColaInfoResponse(
        String nombre,
        int mensajes,
        int consumidores
) {
}
