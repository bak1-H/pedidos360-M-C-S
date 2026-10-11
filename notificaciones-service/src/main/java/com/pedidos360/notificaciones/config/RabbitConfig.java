package com.pedidos360.notificaciones.config;

import com.pedidos360.notificaciones.config.RabbitTopologyProperties.Canal;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
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
    public Queue pedidoCreadoQueue() {
        return colaConDeadLetter(topology.pedidoCreado());
    }

    @Bean
    public Queue pedidoCreadoDeadLetterQueue() {
        return colaMuerta(topology.pedidoCreado());
    }

    @Bean
    public Binding pedidoCreadoBinding() {
        return enlaceDeEvento(pedidoCreadoQueue(), topology.pedidoCreado());
    }

    @Bean
    public Binding pedidoCreadoDeadLetterBinding() {
        return enlaceMuerto(pedidoCreadoDeadLetterQueue(), topology.pedidoCreado());
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
