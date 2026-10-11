package com.pedidos360.notificaciones.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ListenerConEncabezadoAjenoTest {

    private static final long TAG = 11L;

    @Autowired
    private RabbitListenerEndpointRegistry registry;

    @Autowired
    private ObjectMapper objectMapper;

    private ChannelAwareMessageListener listenerDe(String cola) {
        for (MessageListenerContainer contenedor : registry.getListenerContainers()) {
            AbstractMessageListenerContainer real = (AbstractMessageListenerContainer) contenedor;
            if (Arrays.asList(real.getQueueNames()).contains(cola)) {
                return (ChannelAwareMessageListener) real.getMessageListener();
            }
        }
        throw new IllegalStateException("No hay listener para la cola " + cola);
    }

    private Map<String, Map<String, Object>> cuerposPorCola() {
        Map<String, Map<String, Object>> cuerpos = new LinkedHashMap<>();
        cuerpos.put("notificaciones.pedido.creado", Map.of(
                "pedidoId", UUID.randomUUID(), "clienteId", UUID.randomUUID(),
                "direccionDestino", "Calle Falsa 456", "fechaCreacion", LocalDateTime.now()));
        return cuerpos;
    }

    private Message mensajeAjeno(String cola, Object cuerpo) throws Exception {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        propiedades.setHeader("__TypeId__", "com.pedidos360.remitente.inexistente.EventoAjeno");
        propiedades.setDeliveryTag(TAG);
        propiedades.setRedelivered(false);
        propiedades.setConsumerQueue(cola);
        return new Message(objectMapper.writeValueAsBytes(cuerpo), propiedades);
    }

    @Test
    void cadaConsumidorDeNegocioConfirmaUnMensajeConEncabezadoAjeno() throws Exception {
        for (Map.Entry<String, Map<String, Object>> entrada : cuerposPorCola().entrySet()) {
            Channel channel = mock(Channel.class);

            listenerDe(entrada.getKey()).onMessage(mensajeAjeno(entrada.getKey(), entrada.getValue()), channel);

            verify(channel).basicAck(TAG, false);
            verifyNoMoreInteractions(channel);
        }
    }

    @Test
    void cadaListenerDeDlqConfirmaUnMensajeConEncabezadoAjeno() throws Exception {
        for (Map.Entry<String, Map<String, Object>> entrada : cuerposPorCola().entrySet()) {
            Channel channel = mock(Channel.class);
            String dlq = entrada.getKey() + ".dlq";

            listenerDe(dlq).onMessage(mensajeAjeno(dlq, entrada.getValue()), channel);

            verify(channel).basicAck(TAG, false);
            verifyNoMoreInteractions(channel);
        }
    }
}
