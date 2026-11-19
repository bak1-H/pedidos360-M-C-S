package com.pedidos360.pedidos.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pedidos360.pedidos.config.RabbitTopologyProperties;
import com.pedidos360.pedidos.dto.PedidoResponseDTO;
import com.pedidos360.pedidos.event.PedidoCreadoEvent;
import com.pedidos360.pedidos.model.EstadoPedido;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PedidoEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final RabbitTopologyProperties topology = new RabbitTopologyProperties(
            "pedidos360.eventos", "pedidos360.eventos.dlx", new RabbitTopologyProperties.RoutingKeys("pedido.creado"),
            null, null);

    private final PedidoCreadoEvent event = new PedidoCreadoEvent(
            UUID.randomUUID(), UUID.randomUUID(), "Calle Falsa 456", LocalDateTime.now(), null);

    @Test
    void publicaElEventoEnElExchangeConLaRoutingKeyConfigurada() {
        new PedidoEventPublisher(rabbitTemplate, topology).alCrearPedido(event);

        verify(rabbitTemplate).convertAndSend("pedidos360.eventos", "pedido.creado", event);
    }

    @Test
    void elSnapshotParaKafkaNoViajaEnElPayloadDeRabbit() throws Exception {
        PedidoResponseDTO pedido = new PedidoResponseDTO(event.pedidoId(), event.clienteId(), EstadoPedido.CREADO,
                "Origen", "Calle Falsa 456", event.fechaCreacion(), List.of(), null);
        PedidoCreadoEvent conSnapshot = new PedidoCreadoEvent(
                event.pedidoId(), event.clienteId(), "Calle Falsa 456", event.fechaCreacion(), pedido);

        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(conSnapshot);

        assertThat(json).doesNotContain("\"pedido\":").contains("pedidoId");
    }

    @Test
    void unaFallaDelBrokerNoSePropaga() {
        doThrow(new AmqpConnectException(new RuntimeException("broker caido")))
                .when(rabbitTemplate).convertAndSend("pedidos360.eventos", "pedido.creado", event);

        assertThatCode(() -> new PedidoEventPublisher(rabbitTemplate, topology).alCrearPedido(event))
                .doesNotThrowAnyException();
    }
}
