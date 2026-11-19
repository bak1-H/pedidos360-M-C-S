package com.pedidos360.pedidos.event;

import com.pedidos360.pedidos.dto.PedidoResponseDTO;

public record PedidoEstadoCambiadoEvent(PedidoResponseDTO pedido) {
}
