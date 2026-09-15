package com.pedidos360.bff.dto;

import java.util.List;
import java.util.UUID;

public record PerfilResponse(
        UUID id,
        String azureAdObjectId,
        String nombre,
        String email,
        String rol,
        List<String> roles,
        boolean recienCreado) {
}
