package com.pedidos360.notificaciones.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rabbitmq")
public record RabbitTopologyProperties(
        String exchange,
        String deadLetterExchange,
        Canal pedidoCreado
) {

    public record Canal(String queue, String routingKey, String deadLetterQueue, String deadLetterRoutingKey) {
    }
}
