package com.pedidos360.kafkaadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "kafka-admin.protegidos")
public record RecursosProtegidosProperties(List<String> nombres, List<String> prefijos) {

    public RecursosProtegidosProperties {
        nombres = nombres == null ? List.of() : List.copyOf(nombres);
        prefijos = prefijos == null ? List.of() : List.copyOf(prefijos);
    }

    public boolean esProtegido(String nombre) {
        return nombres.contains(nombre) || prefijos.stream().anyMatch(nombre::startsWith);
    }
}
