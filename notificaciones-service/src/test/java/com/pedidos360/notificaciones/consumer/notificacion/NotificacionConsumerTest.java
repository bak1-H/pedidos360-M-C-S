package com.pedidos360.notificaciones.consumer.notificacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pedidos360.notificaciones.config.RabbitTopologyProperties;
import com.pedidos360.notificaciones.config.RabbitTopologyProperties.Canal;
import com.pedidos360.notificaciones.consumer.MensajeProcesador;
import com.pedidos360.notificaciones.model.Notificacion;
import com.pedidos360.notificaciones.model.TipoNotificacion;
import com.pedidos360.notificaciones.repository.NotificacionRepository;
import com.pedidos360.notificaciones.service.NotificacionService;
import com.rabbitmq.client.Channel;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionConsumerTest {

    private static final long TAG = 5L;

    @Mock
    private Channel channel;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private NotificacionRepository notificacionRepository;

    private final Canal canal = new Canal("q.negocio", "rk.negocio", "q.negocio.dlq", "q.negocio.dlq");

    private final RabbitTopologyProperties topology = new RabbitTopologyProperties("ex", "ex.dlx", canal);

    private final MensajeProcesador procesador = new MensajeProcesador(
            new ObjectMapper().registerModule(new JavaTimeModule()),
            Validation.buildDefaultValidatorFactory().getValidator());

    private NotificacionConsumer consumerCon(NotificacionService service) {
        return new NotificacionConsumer(procesador, service, topology);
    }

    private Message mensaje(String cuerpo) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setRedelivered(false);
        return new Message(cuerpo.getBytes(StandardCharsets.UTF_8), propiedades);
    }

    private String pedidoCreado(UUID pedidoId) {
        return "{\"pedidoId\":\"" + pedidoId + "\",\"clienteId\":\"" + UUID.randomUUID()
                + "\",\"direccionDestino\":\"Calle Falsa 456\"}";
    }

    @Test
    void confirmaUnPedidoCreadoValidoYLoRegistra() throws IOException {
        UUID pedidoId = UUID.randomUUID();

        consumerCon(notificacionService).alCrearPedido(mensaje(pedidoCreado(pedidoId)), channel, TAG);

        verify(notificacionService).registrarProcesada(eq(TipoNotificacion.PEDIDO_CREADO), eq(canal),
                anyString(), anyString(), eq(pedidoId));
        verify(channel).basicAck(TAG, false);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void unaFallaAlRegistrarNoCambiaLaConfirmacionDelMensaje() throws IOException {
        when(notificacionRepository.save(any(Notificacion.class))).thenThrow(new IllegalStateException("base caida"));
        NotificacionConsumer consumer = consumerCon(new NotificacionService(notificacionRepository));

        consumer.alCrearPedido(mensaje(pedidoCreado(UUID.randomUUID())), channel, TAG);

        verify(notificacionRepository).save(any(Notificacion.class));
        verify(channel).basicAck(TAG, false);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void descartaUnPedidoCreadoInvalidoHaciaLaDlqSinRegistrarlo() throws IOException {
        consumerCon(notificacionService)
                .alCrearPedido(mensaje("{\"pedidoId\":null,\"clienteId\":null}"), channel, TAG);

        verify(channel).basicNack(TAG, false, false);
        verifyNoMoreInteractions(channel);
        verifyNoInteractions(notificacionService);
    }
}
