package com.pedidos360.kafkaadmin.dto;

import java.util.List;
import java.util.Map;

public record TopicoDetalleResponse(
        String nombre,
        boolean protegido,
        List<ParticionResponse> particiones,
        Map<String, String> configuracion
) {
}
