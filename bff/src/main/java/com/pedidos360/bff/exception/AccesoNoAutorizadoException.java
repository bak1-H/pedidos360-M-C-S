package com.pedidos360.bff.exception;

/** 403 por no ser dueño del recurso, no por falta de rol. */
public class AccesoNoAutorizadoException extends RuntimeException {

    public AccesoNoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
