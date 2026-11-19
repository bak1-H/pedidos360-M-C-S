package com.pedidos360.pedidos.messaging.kafka;

import com.pedidos360.pedidos.config.KafkaTopicsProperties;
import com.pedidos360.pedidos.event.PedidoCreadoEvent;
import com.pedidos360.pedidos.event.PedidoEstadoCambiadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PedidoKafkaPublisher {

    static final String TIPO_CREADO = "PEDIDO_CREADO";
    static final String TIPO_ESTADO_CAMBIADO = "PEDIDO_ESTADO_CAMBIADO";

    private static final Logger log = LoggerFactory.getLogger(PedidoKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicsProperties topics;

    public PedidoKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate, KafkaTopicsProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCrearPedido(PedidoCreadoEvent event) {
        publicar(PedidoEventoKafka.de(TIPO_CREADO, event.pedido()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCambiarEstado(PedidoEstadoCambiadoEvent event) {
        publicar(PedidoEventoKafka.de(TIPO_ESTADO_CAMBIADO, event.pedido()));
    }

    private void publicar(PedidoEventoKafka evento) {
        String topico = topics.pedidosEventos().nombre();
        try {
            kafkaTemplate.send(topico, evento.pedidoId().toString(), evento)
                    .whenComplete((resultado, falla) -> registrar(topico, evento, falla));
        } catch (RuntimeException ex) {
            log.error("No se pudo publicar el evento {} en {}: {}", evento.eventoId(), topico, ex.getMessage(), ex);
        }
    }

    private void registrar(String topico, PedidoEventoKafka evento, Throwable falla) {
        if (falla != null) {
            log.error("No se pudo publicar el evento {} en {}: {}", evento.eventoId(), topico, falla.getMessage(), falla);
            return;
        }
        log.info("Evento {} ({}) publicado en {} para el pedido {}", evento.eventoId(), evento.tipo(), topico, evento.pedidoId());
    }
}
