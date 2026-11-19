package com.pedidos360.pedidos.config;

import com.pedidos360.pedidos.consumer.PayloadInvalidoException;
import com.pedidos360.pedidos.consumer.kafka.EnvioTrackingEvento;
import com.pedidos360.pedidos.consumer.kafka.PedidoEventoKafka;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaProperties kafkaProperties) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                plantillaDeadLetter(kafkaProperties),
                (registro, ex) -> new TopicPartition(registro.topic() + ".DLT", registro.partition()));
        return crearErrorHandler(recoverer);
    }

    static DefaultErrorHandler crearErrorHandler(ConsumerRecordRecoverer recoverer) {
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOffExponencial());
        handler.addNotRetryableExceptions(PayloadInvalidoException.class);
        handler.setRetryListeners(new RegistroDeReintentos());
        return handler;
    }

    private static ExponentialBackOffWithMaxRetries backOffExponencial() {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10000L);
        return backOff;
    }

    private KafkaTemplate<String, Object> plantillaDeadLetter(KafkaProperties kafkaProperties) {
        DelegatingByTypeSerializer valores = new DelegatingByTypeSerializer(
                Map.of(byte[].class, new ByteArraySerializer(), Object.class, new JsonSerializer<>()), true);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                kafkaProperties.buildProducerProperties(null), new StringSerializer(), valores));
    }

    private static final class RegistroDeReintentos implements RetryListener {

        @Override
        public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
            log.warn("Falla procesando {}-{}@{} clave={} eventoId={} intento={}: {}",
                    record.topic(), record.partition(), record.offset(), record.key(),
                    eventoId(record.value()), deliveryAttempt, ex.getMessage());
        }

        @Override
        public void recovered(ConsumerRecord<?, ?> record, Exception ex) {
            log.error("Registro {}-{}@{} clave={} eventoId={} enviado a {}.DLT: {}",
                    record.topic(), record.partition(), record.offset(), record.key(),
                    eventoId(record.value()), record.topic(), ex.getMessage());
        }

        private static String eventoId(Object valor) {
            return switch (valor) {
                case EnvioTrackingEvento evento -> evento.eventoId();
                case PedidoEventoKafka evento -> evento.eventoId();
                case null, default -> "n/d";
            };
        }
    }
}
