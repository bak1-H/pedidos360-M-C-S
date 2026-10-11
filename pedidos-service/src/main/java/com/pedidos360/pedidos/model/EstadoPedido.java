package com.pedidos360.pedidos.model;

public enum EstadoPedido {
    CREADO,
    DESPACHADO,
    ENTREGADO,
    CANCELADO;

    public boolean puedeAvanzarA(EstadoPedido destino) {
        return switch (this) {
            case CREADO -> destino == DESPACHADO || destino == ENTREGADO;
            case DESPACHADO -> destino == ENTREGADO;
            case ENTREGADO, CANCELADO -> false;
        };
    }
}
