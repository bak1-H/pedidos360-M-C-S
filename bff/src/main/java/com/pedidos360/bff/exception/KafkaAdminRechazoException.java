package com.pedidos360.bff.exception;

public class KafkaAdminRechazoException extends RuntimeException {

    private final int status;

    public KafkaAdminRechazoException(int status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
