package com.pedidos360.pedidos.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.pedidos.config.RabbitTopologyProperties.Canal;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitTopologyProperties.class)
public class RabbitConfig {

    private final RabbitTopologyProperties topology;

    public RabbitConfig(RabbitTopologyProperties topology) {
        this.topology = topology;
    }

    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(topology.exchange());
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(topology.deadLetterExchange());
    }

    @Bean
    public RabbitTemplateCustomizer jsonRabbitTemplateCustomizer(ObjectMapper objectMapper) {
        return template -> template.setMessageConverter(new Jackson2JsonMessageConverter(objectMapper));
    }

    @Bean
    public Queue envioAsignadoQueue() {
        return colaConDeadLetter(topology.envioAsignado());
    }

    @Bean
    public Queue envioEstadoCambiadoQueue() {
        return colaConDeadLetter(topology.envioEstadoCambiado());
    }

    @Bean
    public Queue envioAsignadoDeadLetterQueue() {
        return colaMuerta(topology.envioAsignado());
    }

    @Bean
    public Queue envioEstadoCambiadoDeadLetterQueue() {
        return colaMuerta(topology.envioEstadoCambiado());
    }

    @Bean
    public Binding envioAsignadoBinding() {
        return enlaceDeEvento(envioAsignadoQueue(), topology.envioAsignado());
    }

    @Bean
    public Binding envioEstadoCambiadoBinding() {
        return enlaceDeEvento(envioEstadoCambiadoQueue(), topology.envioEstadoCambiado());
    }

    @Bean
    public Binding envioAsignadoDeadLetterBinding() {
        return enlaceMuerto(envioAsignadoDeadLetterQueue(), topology.envioAsignado());
    }

    @Bean
    public Binding envioEstadoCambiadoDeadLetterBinding() {
        return enlaceMuerto(envioEstadoCambiadoDeadLetterQueue(), topology.envioEstadoCambiado());
    }

    private Queue colaConDeadLetter(Canal canal) {
        return QueueBuilder.durable(canal.queue())
                .deadLetterExchange(topology.deadLetterExchange())
                .deadLetterRoutingKey(canal.deadLetterRoutingKey())
                .build();
    }

    private Queue colaMuerta(Canal canal) {
        return QueueBuilder.durable(canal.deadLetterQueue()).build();
    }

    private Binding enlaceDeEvento(Queue queue, Canal canal) {
        return BindingBuilder.bind(queue).to(eventosExchange()).with(canal.routingKey());
    }

    private Binding enlaceMuerto(Queue deadLetterQueue, Canal canal) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange()).with(canal.deadLetterRoutingKey());
    }
}
