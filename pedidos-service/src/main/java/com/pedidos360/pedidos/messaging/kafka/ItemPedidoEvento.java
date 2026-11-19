package com.pedidos360.pedidos.messaging.kafka;

import java.math.BigDecimal;

public record ItemPedidoEvento(String descripcion, int cantidad, BigDecimal valorDeclarado) {
}
