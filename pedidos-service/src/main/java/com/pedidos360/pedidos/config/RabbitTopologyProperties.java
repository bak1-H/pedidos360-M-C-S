package com.pedidos360.pedidos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rabbitmq")
public record RabbitTopologyProperties(
        String exchange,
        String deadLetterExchange,
        RoutingKeys routingKeys,
        Canal envioAsignado,
        Canal envioEstadoCambiado
) {

    public record RoutingKeys(String pedidoCreado) {
    }

    public record Canal(String queue, String routingKey, String deadLetterQueue, String deadLetterRoutingKey) {
    }
}
