package com.pedidos360.pedidos.event;

import com.pedidos360.pedidos.model.EstadoPedido;

import java.util.UUID;

public record PedidoEstadoCambiadoEvent(UUID pedidoId, UUID clienteId, EstadoPedido estado) {
}
