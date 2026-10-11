package com.pedidos360.usuarios.messaging;

import com.pedidos360.usuarios.config.RabbitTopologyProperties;
import com.pedidos360.usuarios.event.UsuarioCreadoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTopologyProperties topology;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCrearUsuario(UsuarioCreadoEvent event) {
        publicar(topology.routingKeys().usuarioCreado(), event);
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
