package com.pedidos360.envios.exception;

import com.pedidos360.envios.model.EstadoEnvio;

public class TransicionInvalidaException extends RuntimeException {

    public TransicionInvalidaException(EstadoEnvio actual, EstadoEnvio destino) {
        super("No se puede cambiar un envío de " + actual + " a " + destino);
    }
}
