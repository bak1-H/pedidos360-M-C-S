package com.pedidos360.usuarios.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rabbitmq")
public record RabbitTopologyProperties(String exchange, RoutingKeys routingKeys) {

    public record RoutingKeys(String usuarioCreado) {
    }
}
