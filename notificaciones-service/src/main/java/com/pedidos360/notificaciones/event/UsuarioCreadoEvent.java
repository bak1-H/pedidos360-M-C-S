package com.pedidos360.notificaciones.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record UsuarioCreadoEvent(
        @NotNull UUID usuarioId,
        @NotBlank String nombre,
        @NotBlank String email,
        String rol,
        LocalDateTime fechaCreacion
) {
}
