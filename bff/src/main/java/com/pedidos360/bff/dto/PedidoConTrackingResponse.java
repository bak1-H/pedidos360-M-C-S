package com.pedidos360.bff.dto;

public record PedidoConTrackingResponse(
        PedidoResponseDto pedido,
        EnvioResponseDto envio) {
}
