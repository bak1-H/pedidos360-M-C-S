package com.pedidos360.pedidos.dto;

import com.pedidos360.pedidos.model.EstadoPedido;
import com.pedidos360.pedidos.model.Pedido;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record PedidoResponseDTO(
        UUID id,
        UUID clienteId,
        EstadoPedido estado,
        String direccionOrigen,
        String direccionDestino,
        LocalDateTime fechaCreacion,
        List<DetallePedidoDTO> detalles,
        PaqueteDTO paquete
) {

    public static PedidoResponseDTO from(Pedido pedido) {
        List<DetallePedidoDTO> detalles = pedido.getDetalles().stream()
                .map(d -> new DetallePedidoDTO(d.getDescripcion(), d.getCantidad(), d.getValorDeclarado()))
                .collect(Collectors.toList());

        PaqueteDTO paqueteDTO = pedido.getPaquete() == null ? null : new PaqueteDTO(
                pedido.getPaquete().getPesoKg(),
                pedido.getPaquete().getAlturaCm(),
                pedido.getPaquete().getAnchoCm(),
                pedido.getPaquete().getLargoCm(),
                pedido.getPaquete().getTipo()
        );

        return new PedidoResponseDTO(
                pedido.getId(),
                pedido.getClienteId(),
                pedido.getEstado(),
                pedido.getDireccionOrigen(),
                pedido.getDireccionDestino(),
                pedido.getFechaCreacion(),
                detalles,
                paqueteDTO
        );
    }
}
