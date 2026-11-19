package com.pedidos360.kafkaadmin.dto;

import java.util.List;

public record MiembroResponse(String miembroId, String clienteId, String host, List<String> asignacion) {
}
