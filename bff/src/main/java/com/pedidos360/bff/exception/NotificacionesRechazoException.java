package com.pedidos360.bff.exception;

public class NotificacionesRechazoException extends RuntimeException {

    private final int status;

    public NotificacionesRechazoException(int status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
