package com.pedidos360.envios.messaging;

import com.pedidos360.envios.config.RabbitTopologyProperties;
import com.pedidos360.envios.event.EnvioEntregadoEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnvioEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final RabbitTopologyProperties topology = new RabbitTopologyProperties(
            "pedidos360.eventos",
            new RabbitTopologyProperties.RoutingKeys("envio.asignado", "envio.estado_cambiado", "envio.entregado"));

    private final EnvioEntregadoEvent event = new EnvioEntregadoEvent(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now());

    @Test
    void publicaElEventoDeEntregaConSuRoutingKey() {
        new EnvioEventPublisher(rabbitTemplate, topology).alEntregarEnvio(event);

        verify(rabbitTemplate).convertAndSend("pedidos360.eventos", "envio.entregado", event);
    }

    @Test
    void unaFallaDelBrokerNoSePropaga() {
        doThrow(new AmqpConnectException(new RuntimeException("broker caido")))
                .when(rabbitTemplate).convertAndSend("pedidos360.eventos", "envio.entregado", event);

        assertThatCode(() -> new EnvioEventPublisher(rabbitTemplate, topology).alEntregarEnvio(event))
                .doesNotThrowAnyException();
    }
}
