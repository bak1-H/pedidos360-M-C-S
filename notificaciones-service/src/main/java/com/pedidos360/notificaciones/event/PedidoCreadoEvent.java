package com.pedidos360.notificaciones.event;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record PedidoCreadoEvent(
        @NotNull UUID pedidoId,
        @NotNull UUID clienteId,
        String direccionDestino,
        LocalDateTime fechaCreacion
) {
}
