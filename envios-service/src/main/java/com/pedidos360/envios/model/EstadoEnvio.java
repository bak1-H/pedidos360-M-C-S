package com.pedidos360.envios.model;

import java.util.EnumSet;
import java.util.Set;

public enum EstadoEnvio {
    PENDIENTE,
    EN_TRANSITO,
    ENTREGADO,
    FALLIDO;

    public Set<EstadoEnvio> destinosPermitidos() {
        return switch (this) {
            case PENDIENTE -> EnumSet.of(EN_TRANSITO, FALLIDO);
            case EN_TRANSITO -> EnumSet.of(ENTREGADO, FALLIDO);
            case ENTREGADO, FALLIDO -> EnumSet.noneOf(EstadoEnvio.class);
        };
    }

    public boolean puedePasarA(EstadoEnvio destino) {
        return destinosPermitidos().contains(destino);
    }
}
