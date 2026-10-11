package com.pedidos360.bff.exception;

public class PedidoNoAsignableException extends RuntimeException {

    public PedidoNoAsignableException() {
        super("El pedido no puede asignarse a un envío porque no existe o ya no está pendiente de despacho");
    }
}
