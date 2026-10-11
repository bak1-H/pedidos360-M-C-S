package com.pedidos360.notificaciones.consumer.dlq;

import com.pedidos360.notificaciones.config.RabbitTopologyProperties;
import com.pedidos360.notificaciones.config.RabbitTopologyProperties.Canal;
import com.pedidos360.notificaciones.model.TipoNotificacion;
import com.pedidos360.notificaciones.service.NotificacionService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class DeadLetterConsumerTest {

    private static final long TAG = 9L;

    @Mock
    private Channel channel;

    @Mock
    private NotificacionService notificacionService;

    private final Canal canal = new Canal("q", "rk", "q.dlq", "q.dlq");

    private final RabbitTopologyProperties topology = new RabbitTopologyProperties("ex", "ex.dlx", canal);

    private DeadLetterConsumer consumer() {
        return new DeadLetterConsumer(topology, notificacionService);
    }

    private Message muerto(MessageProperties propiedades) {
        return new Message("{}".getBytes(StandardCharsets.UTF_8), propiedades);
    }

    @Test
    void registraLaNotificacionMuertaConElMotivoDelXDeathYConfirmaElMensaje() throws IOException {
        MessageProperties propiedades = new MessageProperties();
        propiedades.getHeaders().put("x-death", List.of(Map.of("reason", "rejected", "count", 1L, "queue", "q")));

        consumer().pedidoCreadoMuerto(muerto(propiedades), channel, TAG);

        verify(notificacionService).registrarMuerta(TipoNotificacion.PEDIDO_CREADO, canal, "rejected");
        verify(channel).basicAck(TAG, false);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void usaMotivoDesconocidoYConfirmaSiElMensajeNoTraeXDeath() throws IOException {
        consumer().pedidoCreadoMuerto(muerto(new MessageProperties()), channel, TAG);

        verify(notificacionService).registrarMuerta(TipoNotificacion.PEDIDO_CREADO, canal, "desconocido");
        verify(channel).basicAck(TAG, false);
        verifyNoMoreInteractions(channel);
    }
}
