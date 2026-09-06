package com.pedidos360.bff.dto;

/** Lo que el BFF envia a usuarios-service para aprovisionar un perfil nuevo. */
public record CrearUsuarioRequest(
        String azureAdObjectId,
        String nombre,
        String email,
        String rol) {
}
