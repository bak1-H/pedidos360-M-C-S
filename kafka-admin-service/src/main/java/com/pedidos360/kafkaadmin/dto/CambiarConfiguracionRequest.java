package com.pedidos360.kafkaadmin.dto;

import java.util.List;
import java.util.Map;

public record CambiarConfiguracionRequest(Map<String, String> establecer, List<String> eliminar) {
}
