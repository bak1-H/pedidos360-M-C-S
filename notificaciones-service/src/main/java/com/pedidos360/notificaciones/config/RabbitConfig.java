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
    public Queue envioAsignadoQueue() {
        return colaConDeadLetter(topology.envioAsignado());
    }

    @Bean
    public Queue usuarioCreadoQueue() {
        return colaConDeadLetter(topology.usuarioCreado());
    }

    @Bean
    public Queue envioEntregadoQueue() {
        return colaConDeadLetter(topology.envioEntregado());
    }

    @Bean
    public Queue pedidoCreadoDeadLetterQueue() {
        return colaMuerta(topology.pedidoCreado());
    }

    @Bean
    public Queue envioAsignadoDeadLetterQueue() {
        return colaMuerta(topology.envioAsignado());
    }

    @Bean
    public Queue usuarioCreadoDeadLetterQueue() {
        return colaMuerta(topology.usuarioCreado());
    }

    @Bean
    public Queue envioEntregadoDeadLetterQueue() {
        return colaMuerta(topology.envioEntregado());
    }

    @Bean
    public Queue pedidoCreadoParkedQueue() {
        return colaEstacionada(topology.pedidoCreado());
    }

    @Bean
    public Queue envioAsignadoParkedQueue() {
        return colaEstacionada(topology.envioAsignado());
    }

    @Bean
    public Queue usuarioCreadoParkedQueue() {
        return colaEstacionada(topology.usuarioCreado());
    }

    @Bean
    public Queue envioEntregadoParkedQueue() {
        return colaEstacionada(topology.envioEntregado());
    }

    @Bean
    public Binding pedidoCreadoBinding() {
        return enlaceDeEvento(pedidoCreadoQueue(), topology.pedidoCreado());
    }

    @Bean
    public Binding envioAsignadoBinding() {
        return enlaceDeEvento(envioAsignadoQueue(), topology.envioAsignado());
    }

    @Bean
    public Binding usuarioCreadoBinding() {
        return enlaceDeEvento(usuarioCreadoQueue(), topology.usuarioCreado());
    }

    @Bean
    public Binding envioEntregadoBinding() {
        return enlaceDeEvento(envioEntregadoQueue(), topology.envioEntregado());
    }

    @Bean
    public Binding pedidoCreadoDeadLetterBinding() {
        return enlaceMuerto(pedidoCreadoDeadLetterQueue(), topology.pedidoCreado());
    }

    @Bean
    public Binding envioAsignadoDeadLetterBinding() {
        return enlaceMuerto(envioAsignadoDeadLetterQueue(), topology.envioAsignado());
    }

    @Bean
    public Binding usuarioCreadoDeadLetterBinding() {
        return enlaceMuerto(usuarioCreadoDeadLetterQueue(), topology.usuarioCreado());
    }

    @Bean
    public Binding envioEntregadoDeadLetterBinding() {
        return enlaceMuerto(envioEntregadoDeadLetterQueue(), topology.envioEntregado());
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

    private Queue colaEstacionada(Canal canal) {
        return QueueBuilder.durable(canal.parkedQueue()).build();
    }

    private Binding enlaceDeEvento(Queue queue, Canal canal) {
        return BindingBuilder.bind(queue).to(eventosExchange()).with(canal.routingKey());
    }

    private Binding enlaceMuerto(Queue deadLetterQueue, Canal canal) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange()).with(canal.deadLetterRoutingKey());
    }
}
