package com.pedidos360.usuarios.dto;

import com.pedidos360.usuarios.model.Rol;
import com.pedidos360.usuarios.model.Usuario;

import java.time.LocalDateTime;
import java.util.UUID;

/** Nunca exponemos la entidad JPA en el controller (convencion del SDD, seccion 9). */
public record UsuarioResponse(
        UUID id,
        String azureAdObjectId,
        String nombre,
        String email,
        Rol rol,
        LocalDateTime fechaCreacion) {

    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getAzureAdObjectId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getFechaCreacion());
    }
}
