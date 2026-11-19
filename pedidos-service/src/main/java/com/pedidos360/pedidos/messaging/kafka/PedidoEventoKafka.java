package com.pedidos360.pedidos.messaging.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pedidos360.pedidos.dto.PedidoResponseDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PedidoEventoKafka(
        String eventoId,
        String tipo,
        UUID pedidoId,
        UUID clienteId,
        String estado,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant ocurridoEn,
        String direccionOrigen,
        String direccionDestino,
        int cantidadItems,
        BigDecimal valorDeclaradoTotal,
        String tipoPaquete,
        BigDecimal pesoKg,
        List<ItemPedidoEvento> items
) {

    public static PedidoEventoKafka de(String tipo, PedidoResponseDTO pedido) {
        List<ItemPedidoEvento> items = pedido.detalles().stream()
                .map(d -> new ItemPedidoEvento(d.descripcion(), d.cantidad(), d.valorDeclarado()))
                .toList();
        return new PedidoEventoKafka(
                UUID.randomUUID().toString(),
                tipo,
                pedido.id(),
                pedido.clienteId(),
                pedido.estado().name(),
                Instant.now(),
                pedido.direccionOrigen(),
                pedido.direccionDestino(),
                items.stream().mapToInt(ItemPedidoEvento::cantidad).sum(),
                items.stream().map(i -> i.valorDeclarado().multiply(BigDecimal.valueOf(i.cantidad())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                pedido.paquete() == null ? null : pedido.paquete().tipo().name(),
                pedido.paquete() == null ? null : pedido.paquete().pesoKg(),
                items);
    }
}
