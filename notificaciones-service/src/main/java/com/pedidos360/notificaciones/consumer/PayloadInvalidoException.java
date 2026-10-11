package com.pedidos360.notificaciones.consumer;

public class PayloadInvalidoException extends RuntimeException {

    public PayloadInvalidoException(String message) {
        super(message);
    }

    public PayloadInvalidoException(String message, Throwable cause) {
        super(message, cause);
    }
}
