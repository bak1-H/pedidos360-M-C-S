package com.pedidos360.bff.dto;

/** GET /bff/pedidos/{id}: el pedido mas el tracking de envios-service, si ya despacho. */
public record PedidoConTrackingResponse(
        PedidoResponseDto pedido,
        EnvioResponseDto envio) {
}
