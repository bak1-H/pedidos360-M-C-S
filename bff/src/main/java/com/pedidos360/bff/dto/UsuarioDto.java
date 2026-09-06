package com.pedidos360.bff.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Espejo de lo que devuelve usuarios-service en /internal/usuarios. */
public record UsuarioDto(
        UUID id,
        String azureAdObjectId,
        String nombre,
        String email,
        String rol,
        LocalDateTime fechaCreacion) {
}
