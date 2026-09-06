package com.pedidos360.usuarios.dto;

import com.pedidos360.usuarios.model.Rol;
import jakarta.validation.constraints.NotNull;

public record ActualizarRolRequest(
        @NotNull(message = "rol es obligatorio")
        Rol rol) {
}
