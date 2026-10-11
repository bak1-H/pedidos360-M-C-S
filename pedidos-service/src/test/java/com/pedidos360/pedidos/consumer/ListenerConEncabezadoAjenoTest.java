package com.pedidos360.pedidos.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.pedidos.model.EstadoPedido;
import com.pedidos360.pedidos.model.Pedido;
import com.pedidos360.pedidos.repository.PedidoRepository;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ListenerConEncabezadoAjenoTest {

    private static final long TAG = 7L;
    private static final String COLA_ASIGNADO = "pedidos.envio.asignado";
    private static final String COLA_ESTADO = "pedidos.envio.estado_cambiado";

    private record EnvioAsignadoDeEnvios(UUID envioId, UUID pedidoId, UUID repartidorId,
                                         LocalDateTime fechaEntregaEstimada) {
    }

    private record EnvioEstadoCambiadoDeEnvios(UUID envioId, UUID pedidoId, String estadoEnvio,
                                               String descripcion, LocalDateTime fecha) {
    }

    @Autowired
    private RabbitListenerEndpointRegistry registry;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    private ChannelAwareMessageListener listenerDe(String cola) {
        for (MessageListenerContainer contenedor : registry.getListenerContainers()) {
            AbstractMessageListenerContainer real = (AbstractMessageListenerContainer) contenedor;
            if (Arrays.asList(real.getQueueNames()).contains(cola)) {
                return (ChannelAwareMessageListener) real.getMessageListener();
            }
        }
        throw new IllegalStateException("No hay listener para la cola " + cola);
    }

    private Message mensajeDeEnvios(String claseRemitente, Object cuerpo, String cola) throws Exception {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        propiedades.setHeader("__TypeId__", claseRemitente);
        propiedades.setDeliveryTag(TAG);
        propiedades.setRedelivered(false);
        propiedades.setConsumerQueue(cola);
        return new Message(objectMapper.writeValueAsBytes(cuerpo), propiedades);
    }

    private Pedido pedidoNuevo() {
        Pedido pedido = new Pedido();
        pedido.setClienteId(UUID.randomUUID());
        pedido.setDireccionOrigen("Origen");
        pedido.setDireccionDestino("Destino");
        return pedidoRepository.saveAndFlush(pedido);
    }

    private EstadoPedido estadoDe(Pedido pedido) {
        return pedidoRepository.findById(pedido.getId()).orElseThrow().getEstado();
    }

    @Test
    void elEnvioAsignadoDeEnviosAvanzaElPedidoAunqueSuClaseNoExistaAqui() throws Exception {
        Pedido pedido = pedidoNuevo();
        Channel channel = mock(Channel.class);
        Message mensaje = mensajeDeEnvios("com.pedidos360.envios.event.EnvioAsignadoEvent",
                new EnvioAsignadoDeEnvios(UUID.randomUUID(), pedido.getId(), UUID.randomUUID(), null), COLA_ASIGNADO);

        listenerDe(COLA_ASIGNADO).onMessage(mensaje, channel);

        assertThat(estadoDe(pedido)).isEqualTo(EstadoPedido.DESPACHADO);
        verify(channel).basicAck(TAG, false);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void elCambioDeEstadoEnTransitoDeEnviosDespachaElPedido() throws Exception {
        Pedido pedido = pedidoNuevo();
        Channel channel = mock(Channel.class);
        Message mensaje = mensajeDeEnvios("com.pedidos360.envios.event.EnvioEstadoCambiadoEvent",
                new EnvioEstadoCambiadoDeEnvios(UUID.randomUUID(), pedido.getId(), "EN_TRANSITO", "Salio a reparto",
                        LocalDateTime.now()), COLA_ESTADO);

        listenerDe(COLA_ESTADO).onMessage(mensaje, channel);

        assertThat(estadoDe(pedido)).isEqualTo(EstadoPedido.DESPACHADO);
        verify(channel).basicAck(TAG, false);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void elCambioDeEstadoEntregadoDeEnviosEntregaElPedido() throws Exception {
        Pedido pedido = pedidoNuevo();
        Channel channel = mock(Channel.class);
        Message mensaje = mensajeDeEnvios("com.pedidos360.envios.event.EnvioEstadoCambiadoEvent",
                new EnvioEstadoCambiadoDeEnvios(UUID.randomUUID(), pedido.getId(), "ENTREGADO", "Entregado",
                        LocalDateTime.now()), COLA_ESTADO);

        listenerDe(COLA_ESTADO).onMessage(mensaje, channel);

        assertThat(estadoDe(pedido)).isEqualTo(EstadoPedido.ENTREGADO);
        verify(channel).basicAck(TAG, false);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void losListenersDeDlqConfirmanUnMensajeConEncabezadoAjeno() throws Exception {
        Channel channel = mock(Channel.class);
        Message asignado = mensajeDeEnvios("com.pedidos360.envios.event.EnvioAsignadoEvent",
                new EnvioAsignadoDeEnvios(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null),
                COLA_ASIGNADO + ".dlq");
        Message estado = mensajeDeEnvios("com.pedidos360.envios.event.EnvioEstadoCambiadoEvent",
                new EnvioEstadoCambiadoDeEnvios(UUID.randomUUID(), UUID.randomUUID(), "ENTREGADO", "x",
                        LocalDateTime.now()), COLA_ESTADO + ".dlq");

        listenerDe(COLA_ASIGNADO + ".dlq").onMessage(asignado, channel);
        listenerDe(COLA_ESTADO + ".dlq").onMessage(estado, channel);

        verify(channel, times(2)).basicAck(TAG, false);
        verifyNoMoreInteractions(channel);
    }
}
