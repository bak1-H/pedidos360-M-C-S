package com.pedidos360.bff.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Espejo de lo que devuelve pedidos-service en /internal/pedidos. */
public record PedidoResponseDto(
        UUID id,
        UUID clienteId,
        String estado,
        String direccionOrigen,
        String direccionDestino,
        LocalDateTime fechaCreacion,
        List<DetallePedidoDto> detalles,
        PaqueteDto paquete) {
}
