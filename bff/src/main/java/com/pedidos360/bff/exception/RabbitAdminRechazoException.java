package com.pedidos360.bff.exception;

public class RabbitAdminRechazoException extends RuntimeException {

    private final int status;

    public RabbitAdminRechazoException(int status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
