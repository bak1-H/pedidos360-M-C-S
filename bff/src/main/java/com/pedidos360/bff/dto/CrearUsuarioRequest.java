package com.pedidos360.bff.dto;

public record CrearUsuarioRequest(
        String azureAdObjectId,
        String nombre,
        String email,
        String rol) {
}
