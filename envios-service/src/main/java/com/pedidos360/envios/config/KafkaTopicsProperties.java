package com.pedidos360.envios.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka.topics")
public record KafkaTopicsProperties(Topico enviosTracking) {

    public record Topico(
            String nombre,
            int particiones,
            int replicas,
            int minInsyncReplicas,
            String cleanupPolicy,
            long retentionMs,
            long segmentMs,
            long minCompactionLagMs
    ) {

        public String deadLetter() {
            return nombre + ".DLT";
        }
    }
}
