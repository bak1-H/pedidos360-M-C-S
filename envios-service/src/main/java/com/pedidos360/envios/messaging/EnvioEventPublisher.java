package com.pedidos360.envios.messaging;

import com.pedidos360.envios.config.RabbitTopologyProperties;
import com.pedidos360.envios.event.EnvioAsignadoEvent;
import com.pedidos360.envios.event.EnvioEntregadoEvent;
import com.pedidos360.envios.event.EnvioEstadoCambiadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EnvioEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EnvioEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTopologyProperties topology;

    public EnvioEventPublisher(RabbitTemplate rabbitTemplate, RabbitTopologyProperties topology) {
        this.rabbitTemplate = rabbitTemplate;
        this.topology = topology;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alAsignarEnvio(EnvioAsignadoEvent event) {
        publicar(topology.routingKeys().envioAsignado(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCambiarEstado(EnvioEstadoCambiadoEvent event) {
        publicar(topology.routingKeys().envioEstadoCambiado(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alEntregarEnvio(EnvioEntregadoEvent event) {
        publicar(topology.routingKeys().envioEntregado(), event);
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
