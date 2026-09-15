package com.pedidos360.usuarios.dto;

import com.pedidos360.usuarios.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CrearUsuarioRequest(
        @NotBlank(message = "azureAdObjectId es obligatorio")
        String azureAdObjectId,

        @NotBlank(message = "nombre es obligatorio")
        String nombre,

        @NotBlank(message = "email es obligatorio")
        @Email(message = "email no tiene formato valido")
        String email,

        Rol rol) {
}
