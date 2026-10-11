package com.pedidos360.usuarios.messaging;

import com.pedidos360.usuarios.config.RabbitTopologyProperties;
import com.pedidos360.usuarios.event.UsuarioCreadoEvent;
import com.pedidos360.usuarios.model.Rol;
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
class UsuarioEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final RabbitTopologyProperties topology = new RabbitTopologyProperties(
            "pedidos360.eventos", new RabbitTopologyProperties.RoutingKeys("usuario.creado"));

    private final UsuarioCreadoEvent event = new UsuarioCreadoEvent(
            UUID.randomUUID(), "Ana Soto", "ana@pedidos360.cl", Rol.CLIENTE, LocalDateTime.now());

    @Test
    void publicaElEventoEnElExchangeConLaRoutingKeyConfigurada() {
        new UsuarioEventPublisher(rabbitTemplate, topology).alCrearUsuario(event);

        verify(rabbitTemplate).convertAndSend("pedidos360.eventos", "usuario.creado", event);
    }

    @Test
    void unaFallaDelBrokerNoSePropaga() {
        doThrow(new AmqpConnectException(new RuntimeException("broker caido")))
                .when(rabbitTemplate).convertAndSend("pedidos360.eventos", "usuario.creado", event);

        assertThatCode(() -> new UsuarioEventPublisher(rabbitTemplate, topology).alCrearUsuario(event))
                .doesNotThrowAnyException();
    }
}
