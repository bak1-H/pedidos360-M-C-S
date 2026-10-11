package com.pedidos360.notificaciones.dto;

public record ColaResponse(
        String nombre,
        CategoriaCola categoria,
        int mensajes,
        int consumidores
) {
}
