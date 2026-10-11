package com.pedidos360.notificaciones.consumer.dlq;

import com.pedidos360.notificaciones.config.RabbitTopologyProperties;
import com.pedidos360.notificaciones.config.RabbitTopologyProperties.Canal;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class DeadLetterConsumerTest {

    private static final long TAG = 9L;
    private static final String PARKED = "notificaciones.pedido.creado.dlq.parked";

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private Channel channel;

    private final Canal canal = new Canal("q", "rk", "q.dlq", "q.dlq", PARKED);

    private final RabbitTopologyProperties topology =
            new RabbitTopologyProperties("ex", "ex.dlx", canal, canal, canal, canal);

    private final Message mensaje = new Message("{}".getBytes(StandardCharsets.UTF_8), new MessageProperties());

    private DeadLetterConsumer consumer() {
        return new DeadLetterConsumer(rabbitTemplate, topology, Duration.ofMillis(100));
    }

    private void brokerResponde(Consumer<CorrelationData> respuesta) {
        doAnswer(invocation -> {
            respuesta.accept(invocation.getArgument(3));
            return null;
        }).when(rabbitTemplate).send(any(String.class), any(String.class), any(Message.class), any(CorrelationData.class));
    }

    @Test
    void confirmaElMensajeSoloDespuesDeLaConfirmacionPositivaDelBroker() throws IOException {
        brokerResponde(correlacion -> correlacion.getFuture().complete(new CorrelationData.Confirm(true, null)));

        consumer().pedidoCreadoMuerto(mensaje, channel, TAG);

        InOrder orden = inOrder(rabbitTemplate, channel);
        orden.verify(rabbitTemplate).send(any(String.class), any(String.class), any(Message.class), any(CorrelationData.class));
        orden.verify(channel).basicAck(TAG, false);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void reencolaElMensajeSiElBrokerEnviaConfirmacionNegativa() throws IOException {
        brokerResponde(correlacion -> correlacion.getFuture().complete(new CorrelationData.Confirm(false, "sin espacio")));

        consumer().pedidoCreadoMuerto(mensaje, channel, TAG);

        verify(channel).basicNack(TAG, false, true);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void reencolaElMensajeSiElBrokerLoDevuelve() throws IOException {
        brokerResponde(correlacion -> {
            correlacion.setReturned(new ReturnedMessage(mensaje, 312, "NO_ROUTE", "", PARKED));
            correlacion.getFuture().complete(new CorrelationData.Confirm(true, null));
        });

        consumer().pedidoCreadoMuerto(mensaje, channel, TAG);

        verify(channel).basicNack(TAG, false, true);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void reencolaElMensajeSiLaConfirmacionNoLlegaATiempo() throws IOException {
        consumer().pedidoCreadoMuerto(mensaje, channel, TAG);

        verify(channel).basicNack(TAG, false, true);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void reencolaElMensajeSiElEnvioLanzaCualquierExcepcion() throws IOException {
        doThrow(new AmqpConnectException(new RuntimeException("broker caido")))
                .when(rabbitTemplate).send(any(String.class), any(String.class), any(Message.class), any(CorrelationData.class));

        consumer().pedidoCreadoMuerto(mensaje, channel, TAG);

        doThrow(new IllegalStateException("canal cerrado"))
                .when(rabbitTemplate).send(any(String.class), any(String.class), any(Message.class), any(CorrelationData.class));

        consumer().pedidoCreadoMuerto(mensaje, channel, TAG);

        verify(channel, times(2)).basicNack(TAG, false, true);
        verifyNoMoreInteractions(channel);
    }
}
