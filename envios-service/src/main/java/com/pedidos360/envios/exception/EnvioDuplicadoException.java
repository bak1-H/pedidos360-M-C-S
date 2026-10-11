package com.pedidos360.envios.exception;

import java.util.UUID;

public class EnvioDuplicadoException extends RuntimeException {

    public EnvioDuplicadoException(UUID pedidoId) {
        super("Ya existe un envío para el pedido " + pedidoId);
    }

    public EnvioDuplicadoException(UUID pedidoId, Throwable cause) {
        super("Ya existe un envío para el pedido " + pedidoId, cause);
    }
}
