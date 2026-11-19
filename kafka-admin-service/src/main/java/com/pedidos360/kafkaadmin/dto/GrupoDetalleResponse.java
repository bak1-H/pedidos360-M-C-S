package com.pedidos360.kafkaadmin.dto;

import java.util.List;

public record GrupoDetalleResponse(
        String id,
        String estado,
        List<MiembroResponse> miembros,
        List<OffsetParticionResponse> offsets,
        long lagTotal
) {
}
