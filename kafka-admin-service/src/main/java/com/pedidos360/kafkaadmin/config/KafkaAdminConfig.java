package com.pedidos360.kafkaadmin.config;

import org.apache.kafka.clients.admin.Admin;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
@EnableConfigurationProperties(RecursosProtegidosProperties.class)
public class KafkaAdminConfig {

    @Bean(destroyMethod = "close")
    public Admin adminClient(KafkaAdmin kafkaAdmin) {
        return Admin.create(kafkaAdmin.getConfigurationProperties());
    }
}
