package com.pedidos360.pedidos.messaging;

import com.pedidos360.pedidos.config.RabbitTopologyProperties;
import com.pedidos360.pedidos.event.PedidoCreadoEvent;
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
class PedidoEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final RabbitTopologyProperties topology = new RabbitTopologyProperties(
            "pedidos360.eventos", new RabbitTopologyProperties.RoutingKeys("pedido.creado"));

    private final PedidoCreadoEvent event = new PedidoCreadoEvent(
            UUID.randomUUID(), UUID.randomUUID(), "Calle Falsa 456", LocalDateTime.now());

    @Test
    void publicaElEventoEnElExchangeConLaRoutingKeyConfigurada() {
        new PedidoEventPublisher(rabbitTemplate, topology).alCrearPedido(event);

        verify(rabbitTemplate).convertAndSend("pedidos360.eventos", "pedido.creado", event);
    }

    @Test
    void unaFallaDelBrokerNoSePropaga() {
        doThrow(new AmqpConnectException(new RuntimeException("broker caido")))
                .when(rabbitTemplate).convertAndSend("pedidos360.eventos", "pedido.creado", event);

        assertThatCode(() -> new PedidoEventPublisher(rabbitTemplate, topology).alCrearPedido(event))
                .doesNotThrowAnyException();
    }
}
