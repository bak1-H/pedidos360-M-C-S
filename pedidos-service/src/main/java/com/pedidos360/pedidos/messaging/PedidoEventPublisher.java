package com.pedidos360.pedidos.messaging;

import com.pedidos360.pedidos.config.RabbitTopologyProperties;
import com.pedidos360.pedidos.event.PedidoCreadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PedidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTopologyProperties topology;

    public PedidoEventPublisher(RabbitTemplate rabbitTemplate, RabbitTopologyProperties topology) {
        this.rabbitTemplate = rabbitTemplate;
        this.topology = topology;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCrearPedido(PedidoCreadoEvent event) {
        publicar(topology.routingKeys().pedidoCreado(), event);
    }

    private void publicar(String routingKey, Object event) {
        try {
            rabbitTemplate.convertAndSend(topology.exchange(), routingKey, event);
            log.info("Evento {} publicado en {}", routingKey, topology.exchange());
        } catch (RuntimeException ex) {
            log.error("No se pudo publicar el evento {}: {}", routingKey, ex.getMessage(), ex);
        }
    }
}
