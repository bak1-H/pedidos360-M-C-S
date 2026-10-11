package com.pedidos360.envios.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rabbitmq")
public record RabbitTopologyProperties(String exchange, RoutingKeys routingKeys) {

    public record RoutingKeys(String envioAsignado, String envioEstadoCambiado, String envioEntregado) {
    }
}
