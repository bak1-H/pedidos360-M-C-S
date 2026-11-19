package com.pedidos360.kafkaadmin.dto;

public final class PatronesKafka {

    public static final String NOMBRE = "^[A-Za-z0-9._-]{1,249}$";
    public static final String DESTINO_OFFSETS = "^(earliest|latest)$";

    private PatronesKafka() {
    }
}
