package com.pedidos360.usuarios.dto;

import com.pedidos360.usuarios.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Lo que manda el BFF la primera vez que un usuario entra con su token de Azure AD.
 * El rol es opcional: si no viene, el usuario queda como CLIENTE.
 */
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
