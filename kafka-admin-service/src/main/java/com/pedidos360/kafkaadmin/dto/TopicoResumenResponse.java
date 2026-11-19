package com.pedidos360.kafkaadmin.dto;

public record TopicoResumenResponse(String nombre, int particiones, int factorReplicacion, boolean protegido) {
}
