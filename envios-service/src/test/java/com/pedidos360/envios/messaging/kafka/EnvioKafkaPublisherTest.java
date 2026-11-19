package com.pedidos360.envios.messaging.kafka;

import com.pedidos360.envios.config.KafkaTopicsProperties;
import com.pedidos360.envios.event.EnvioTrackingEvent;
import com.pedidos360.envios.model.EstadoEnvio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnvioKafkaPublisherTest {

    private static final String TOPICO = "pedidos360.envios.tracking";

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private final KafkaTopicsProperties topics = new KafkaTopicsProperties(
            new KafkaTopicsProperties.Topico(TOPICO, 3, 3, 2, "compact,delete", 259200000L, 3600000L, 60000L));

    private final EnvioTrackingEvent event = new EnvioTrackingEvent(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), EstadoEnvio.EN_TRANSITO);

    @Test
    void publicaElTrackingConElPedidoIdComoClave() {
        when(kafkaTemplate.send(eq(TOPICO), eq(event.pedidoId().toString()), any(EnvioTrackingEvento.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

        new EnvioKafkaPublisher(kafkaTemplate, topics).alRegistrarTracking(event);

        verify(kafkaTemplate).send(eq(TOPICO), eq(event.pedidoId().toString()), captor.capture());
        EnvioTrackingEvento evento = (EnvioTrackingEvento) captor.getValue();
        assertThat(evento.estado()).isEqualTo("EN_TRANSITO");
        assertThat(evento.envioId()).isEqualTo(event.envioId());
        assertThat(evento.repartidorId()).isEqualTo(event.repartidorId());
        assertThat(evento.eventoId()).isNotBlank();
    }

    @Test
    void unaFallaDelBrokerNoSePropaga() {
        when(kafkaTemplate.send(eq(TOPICO), eq(event.pedidoId().toString()), any(EnvioTrackingEvento.class)))
                .thenThrow(new IllegalStateException("broker caido"));

        assertThatCode(() -> new EnvioKafkaPublisher(kafkaTemplate, topics).alRegistrarTracking(event))
                .doesNotThrowAnyException();
    }
}
