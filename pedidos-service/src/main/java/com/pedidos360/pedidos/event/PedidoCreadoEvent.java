package com.pedidos360.pedidos.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PedidoCreadoEvent(
        UUID pedidoId,
        UUID clienteId,
        String direccionDestino,
        LocalDateTime fechaCreacion
) {
}
