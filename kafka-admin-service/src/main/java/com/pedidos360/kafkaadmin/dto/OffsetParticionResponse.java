package com.pedidos360.kafkaadmin.dto;

public record OffsetParticionResponse(String topico, int particion, long offsetConfirmado, long offsetFinal, long lag) {
}
