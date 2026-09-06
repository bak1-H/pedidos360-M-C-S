package com.pedidos360.bff.dto;

import java.util.List;
import java.util.UUID;

/** Lo que el BFF envia a POST /internal/pedidos, con el clienteId ya resuelto. */
public record PedidoRequestDto(
        UUID clienteId,
        String direccionOrigen,
        String direccionDestino,
        List<DetallePedidoDto> detalles,
        PaqueteDto paquete) {

    public static PedidoRequestDto desde(UUID clienteId, CrearPedidoRequest request) {
        return new PedidoRequestDto(
                clienteId,
                request.direccionOrigen(),
                request.direccionDestino(),
                request.detalles(),
                request.paquete());
    }
}
