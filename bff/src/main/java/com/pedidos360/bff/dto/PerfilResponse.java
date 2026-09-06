package com.pedidos360.bff.dto;

import java.util.List;
import java.util.UUID;

/**
 * Lo que /bff/me le devuelve a Angular: el perfil guardado en usuarios-service
 * mas los roles que vienen firmados en el token. Angular usa "roles" para
 * mostrar u ocultar secciones del menu.
 */
public record PerfilResponse(
        UUID id,
        String azureAdObjectId,
        String nombre,
        String email,
        String rol,
        List<String> roles,
        boolean recienCreado) {
}
