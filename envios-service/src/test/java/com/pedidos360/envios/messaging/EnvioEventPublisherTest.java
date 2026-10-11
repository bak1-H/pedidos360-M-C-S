package com.pedidos360.envios.messaging;

import com.pedidos360.envios.config.RabbitTopologyProperties;
import com.pedidos360.envios.event.EnvioAsignadoEvent;
import com.pedidos360.envios.event.EnvioEstadoCambiadoEvent;
import com.pedidos360.envios.model.EstadoEnvio;
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
            new RabbitTopologyProperties.RoutingKeys("envio.asignado", "envio.estado_cambiado"));

    private final EnvioAsignadoEvent asignado = new EnvioAsignadoEvent(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now());

    private final EnvioEstadoCambiadoEvent estadoCambiado = new EnvioEstadoCambiadoEvent(
            UUID.randomUUID(), UUID.randomUUID(), EstadoEnvio.EN_TRANSITO, "Salio a reparto", LocalDateTime.now());

    @Test
    void publicaElEnvioAsignadoConSuRoutingKey() {
        new EnvioEventPublisher(rabbitTemplate, topology).alAsignarEnvio(asignado);

        verify(rabbitTemplate).convertAndSend("pedidos360.eventos", "envio.asignado", asignado);
    }

    @Test
    void publicaElCambioDeEstadoConSuRoutingKey() {
        new EnvioEventPublisher(rabbitTemplate, topology).alCambiarEstado(estadoCambiado);

        verify(rabbitTemplate).convertAndSend("pedidos360.eventos", "envio.estado_cambiado", estadoCambiado);
    }

    @Test
    void unaFallaDelBrokerNoSePropaga() {
        doThrow(new AmqpConnectException(new RuntimeException("broker caido")))
                .when(rabbitTemplate).convertAndSend("pedidos360.eventos", "envio.asignado", asignado);

        assertThatCode(() -> new EnvioEventPublisher(rabbitTemplate, topology).alAsignarEnvio(asignado))
                .doesNotThrowAnyException();
    }
}
