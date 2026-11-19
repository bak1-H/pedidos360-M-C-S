package com.pedidos360.kafkaadmin.dto;

import java.util.List;

public record ClusterResponse(String clusterId, NodoResponse controlador, List<NodoResponse> brokers) {
}
