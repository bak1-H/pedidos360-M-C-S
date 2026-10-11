package com.pedidos360.pedidos.consumer.dlq;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class DeadLetterConsumerTest {

    private static final long TAG = 2L;

    @Mock
    private Channel channel;

    private final DeadLetterConsumer consumer = new DeadLetterConsumer();

    @Test
    void confirmaLosMensajesMuertosDeAmbasDlqTrasRegistrarlos() throws IOException {
        MessageProperties propiedades = new MessageProperties();
        propiedades.getHeaders().put("x-death", List.of(Map.of("reason", "rejected", "count", 1L, "queue", "q")));
        Message muerto = new Message("{}".getBytes(StandardCharsets.UTF_8), propiedades);

        consumer.envioAsignadoMuerto(muerto, channel, TAG);
        consumer.envioEstadoCambiadoMuerto(muerto, channel, TAG);

        verify(channel, times(2)).basicAck(TAG, false);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void confirmaUnMensajeMuertoSinEncabezadoXDeath() throws IOException {
        Message muerto = new Message("{}".getBytes(StandardCharsets.UTF_8), new MessageProperties());

        consumer.envioAsignadoMuerto(muerto, channel, TAG);

        verify(channel).basicAck(TAG, false);
        verifyNoMoreInteractions(channel);
    }
}
