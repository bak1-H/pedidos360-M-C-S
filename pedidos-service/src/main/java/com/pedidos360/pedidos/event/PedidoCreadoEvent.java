package com.pedidos360.pedidos.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pedidos360.pedidos.dto.PedidoResponseDTO;

import java.time.LocalDateTime;
import java.util.UUID;

public record PedidoCreadoEvent(
        UUID pedidoId,
        UUID clienteId,
        String direccionDestino,
        LocalDateTime fechaCreacion,
        @JsonIgnore PedidoResponseDTO pedido
) {
}
