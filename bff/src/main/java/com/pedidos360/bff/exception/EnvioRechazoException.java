package com.pedidos360.bff.exception;

public class EnvioRechazoException extends RuntimeException {

    private final int status;
    private final String codigo;

    public EnvioRechazoException(int status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }

    public int getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
