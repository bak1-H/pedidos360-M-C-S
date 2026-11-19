package com.pedidos360.pedidos.consumer.kafka;

import com.pedidos360.pedidos.service.TrackingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EnvioTrackingConsumer {

    private static final Logger log = LoggerFactory.getLogger(EnvioTrackingConsumer.class);

    private final ValidadorPayload validador;
    private final TrackingService trackingService;

    public EnvioTrackingConsumer(ValidadorPayload validador, TrackingService trackingService) {
        this.validador = validador;
        this.trackingService = trackingService;
    }

    @KafkaListener(
            topics = "${kafka.topics.envios-tracking}",
            groupId = "${kafka.grupo}",
            properties = "spring.json.value.default.type=com.pedidos360.pedidos.consumer.kafka.EnvioTrackingEvento")
    public void alRecibirTracking(ConsumerRecord<String, EnvioTrackingEvento> registro) {
        EnvioTrackingEvento evento = validador.validar(registro.value());
        log.info("Tracking {}-{}@{} clave={} eventoId={} estado={}",
                registro.topic(), registro.partition(), registro.offset(), registro.key(),
                evento.eventoId(), evento.estado());
        trackingService.registrar(evento);
    }
}
