package com.pedidos360.usuarios.event;

import com.pedidos360.usuarios.model.Rol;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioCreadoEvent(
        UUID usuarioId,
        String nombre,
        String email,
        Rol rol,
        LocalDateTime fechaCreacion) {
}
