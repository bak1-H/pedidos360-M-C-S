package com.pedidos360.envios.messaging.kafka;

import com.pedidos360.envios.config.KafkaTopicsProperties;
import com.pedidos360.envios.event.EnvioTrackingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

@Component
public class EnvioKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(EnvioKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicsProperties topics;

    public EnvioKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate, KafkaTopicsProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRegistrarTracking(EnvioTrackingEvent event) {
        publicar(new EnvioTrackingEvento(UUID.randomUUID().toString(), event.envioId(), event.pedidoId(),
                event.repartidorId(), event.estado().name(), Instant.now(), event.descripcion(), event.fechaAsignacion(),
                event.fechaEntregaEstimada()));
    }

    private void publicar(EnvioTrackingEvento evento) {
        String topico = topics.enviosTracking().nombre();
        try {
            kafkaTemplate.send(topico, evento.pedidoId().toString(), evento)
                    .whenComplete((resultado, falla) -> registrar(topico, evento, falla));
        } catch (RuntimeException ex) {
            log.error("No se pudo publicar el evento {} en {}: {}", evento.eventoId(), topico, ex.getMessage(), ex);
        }
    }

    private void registrar(String topico, EnvioTrackingEvento evento, Throwable falla) {
        if (falla != null) {
            log.error("No se pudo publicar el evento {} en {}: {}", evento.eventoId(), topico, falla.getMessage(), falla);
            return;
        }
        log.info("Evento {} ({}) publicado en {} para el pedido {}", evento.eventoId(), evento.estado(), topico, evento.pedidoId());
    }
}
