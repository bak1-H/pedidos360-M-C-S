package com.pedidos360.bff.dto;

public final class PatronesRabbit {

    public static final String NOMBRE = "^[A-Za-z0-9._-]{1,255}$";
    public static final String TIPO_EXCHANGE = "^(direct|topic|fanout|headers)$";
    public static final String TIPO_DESTINO = "^(queue|exchange)$";

    private PatronesRabbit() {
    }
}
