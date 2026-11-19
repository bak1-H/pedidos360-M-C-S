package com.pedidos360.pedidos.config;

import com.pedidos360.pedidos.consumer.PayloadInvalidoException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class KafkaConsumerConfigTest {

    private final ConsumerRecordRecoverer recoverer = mock(ConsumerRecordRecoverer.class);
    private final Consumer<?, ?> consumer = mock(Consumer.class);
    private final MessageListenerContainer container = mock(MessageListenerContainer.class);
    private final DefaultErrorHandler handler = KafkaConsumerConfig.crearErrorHandler(recoverer);
    private final ConsumerRecord<String, String> registro = new ConsumerRecord<>("topico", 0, 0L, "clave", "valor");

    @Test
    void payloadInvalidoVaAlRecoveryDeInmediatoSinReintentos() {
        boolean recuperado = handler.handleOne(new PayloadInvalidoException("invalido"), registro, consumer, container);

        assertThat(recuperado).isTrue();
        verify(recoverer, times(1)).accept(any(), any());
    }

    @Test
    void excepcionGenericaSeReintentaTresVecesAntesDelRecovery() {
        RuntimeException falla = new RuntimeException("transitoria");

        assertThat(handler.handleOne(falla, registro, consumer, container)).isFalse();
        assertThat(handler.handleOne(falla, registro, consumer, container)).isFalse();
        assertThat(handler.handleOne(falla, registro, consumer, container)).isFalse();
        verify(recoverer, never()).accept(any(), any());

        assertThat(handler.handleOne(falla, registro, consumer, container)).isTrue();
        verify(recoverer, times(1)).accept(any(), any());
    }
}
