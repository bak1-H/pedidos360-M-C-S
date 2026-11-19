package com.pedidos360.pedidos.consumer.kafka;

import com.pedidos360.pedidos.service.AuditoriaPedidoService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuditoriaPedidoConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaPedidoConsumer.class);

    private final ValidadorPayload validador;
    private final AuditoriaPedidoService auditoria;

    public AuditoriaPedidoConsumer(ValidadorPayload validador, AuditoriaPedidoService auditoria) {
        this.validador = validador;
        this.auditoria = auditoria;
    }

    @KafkaListener(
            topics = "${kafka.topics.pedidos-eventos.nombre}",
            groupId = "${kafka.grupo-auditoria}",
            properties = "spring.json.value.default.type=com.pedidos360.pedidos.consumer.kafka.PedidoEventoKafka")
    public void alRecibirEventoDePedido(ConsumerRecord<String, PedidoEventoKafka> registro) {
        PedidoEventoKafka evento = validador.validar(registro.value());
        log.info("Auditoria {}-{}@{} clave={} eventoId={} tipo={} estado={}",
                registro.topic(), registro.partition(), registro.offset(), registro.key(),
                evento.eventoId(), evento.tipo(), evento.estado());
        auditoria.registrar(evento);
    }
}
