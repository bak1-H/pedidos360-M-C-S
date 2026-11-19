package com.pedidos360.pedidos.config;

import com.pedidos360.pedidos.config.KafkaTopicsProperties.Topico;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class KafkaTopicsConfig {

    private static final long DEAD_LETTER_RETENTION_MS = 1_209_600_000L;

    private final KafkaTopicsProperties topics;

    public KafkaTopicsConfig(KafkaTopicsProperties topics) {
        this.topics = topics;
    }

    @Bean
    public NewTopic pedidosEventosTopic() {
        Topico topico = topics.pedidosEventos();
        return TopicBuilder.name(topico.nombre())
                .partitions(topico.particiones())
                .replicas(topico.replicas())
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(topico.minInsyncReplicas()))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, topico.cleanupPolicy())
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(topico.retentionMs()))
                .build();
    }

    @Bean
    public NewTopic pedidosEventosDeadLetterTopic() {
        Topico topico = topics.pedidosEventos();
        return TopicBuilder.name(topico.deadLetter())
                .partitions(topico.particiones())
                .replicas(topico.replicas())
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, String.valueOf(topico.minInsyncReplicas()))
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(DEAD_LETTER_RETENTION_MS))
                .build();
    }
}
