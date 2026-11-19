package com.pedidos360.notificaciones.consumer.kafka;

import com.pedidos360.notificaciones.service.MetricasStreamingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AnaliticaStreamingConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnaliticaStreamingConsumer.class);

    private final ValidadorPayload validador;
    private final MetricasStreamingService metricas;

    public AnaliticaStreamingConsumer(ValidadorPayload validador, MetricasStreamingService metricas) {
        this.validador = validador;
        this.metricas = metricas;
    }

    @KafkaListener(
            topics = "${kafka.topics.pedidos-eventos}",
            groupId = "${kafka.grupo}",
            properties = "spring.json.value.default.type=com.pedidos360.notificaciones.consumer.kafka.PedidoEventoKafka")
    public void alRecibirEventoDePedido(ConsumerRecord<String, PedidoEventoKafka> registro) {
        PedidoEventoKafka evento = validador.validar(registro.value());
        log.info("Pedido {}-{}@{} clave={} eventoId={} tipo={} estado={}",
                registro.topic(), registro.partition(), registro.offset(), registro.key(),
                evento.eventoId(), evento.tipo(), evento.estado());
        metricas.registrarPedido(evento);
    }

    @KafkaListener(
            topics = "${kafka.topics.envios-tracking}",
            groupId = "${kafka.grupo}",
            properties = "spring.json.value.default.type=com.pedidos360.notificaciones.consumer.kafka.EnvioTrackingEvento")
    public void alRecibirTrackingDeEnvio(ConsumerRecord<String, EnvioTrackingEvento> registro) {
        EnvioTrackingEvento evento = validador.validar(registro.value());
        log.info("Envio {}-{}@{} clave={} eventoId={} estado={}",
                registro.topic(), registro.partition(), registro.offset(), registro.key(),
                evento.eventoId(), evento.estado());
        metricas.registrarEnvio(evento);
    }
}
