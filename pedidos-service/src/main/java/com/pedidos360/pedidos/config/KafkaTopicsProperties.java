package com.pedidos360.pedidos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka.topics")
public record KafkaTopicsProperties(Topico pedidosEventos, String enviosTracking) {

    public record Topico(
            String nombre,
            int particiones,
            int replicas,
            int minInsyncReplicas,
            String cleanupPolicy,
            long retentionMs
    ) {

        public String deadLetter() {
            return nombre + ".DLT";
        }
    }
}
