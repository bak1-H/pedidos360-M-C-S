package com.pedidos360.pedidos.consumer.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class KafkaDeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaDeadLetterConsumer.class);

    @KafkaListener(
            topics = "${kafka.topics.envios-tracking}.DLT",
            groupId = "${kafka.grupo}-dlt",
            properties = "spring.deserializer.value.delegate.class=org.apache.kafka.common.serialization.StringDeserializer")
    public void trackingMuerto(ConsumerRecord<String, String> registro) {
        log.error("Mensaje muerto en {}-{}@{} clave={} topico original={} causa={} valor={}",
                registro.topic(), registro.partition(), registro.offset(), registro.key(),
                encabezado(registro, KafkaHeaders.DLT_ORIGINAL_TOPIC),
                encabezado(registro, KafkaHeaders.DLT_EXCEPTION_MESSAGE),
                registro.value());
    }

    private String encabezado(ConsumerRecord<?, ?> registro, String nombre) {
        Header header = registro.headers().lastHeader(nombre);
        return header == null ? "n/d" : new String(header.value(), StandardCharsets.UTF_8);
    }
}
