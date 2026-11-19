package com.pedidos360.pedidos.messaging.kafka;

import com.pedidos360.pedidos.config.KafkaTopicsProperties;
import com.pedidos360.pedidos.event.PedidoCreadoEvent;
import com.pedidos360.pedidos.event.PedidoEstadoCambiadoEvent;
import com.pedidos360.pedidos.model.EstadoPedido;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoKafkaPublisherTest {

    private static final String TOPICO = "pedidos360.pedidos.eventos";

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private final KafkaTopicsProperties topics = new KafkaTopicsProperties(
            new KafkaTopicsProperties.Topico(TOPICO, 3, 3, 2, "delete", 604800000L), "pedidos360.envios.tracking");

    private final UUID pedidoId = UUID.randomUUID();
    private final UUID clienteId = UUID.randomUUID();

    @Test
    void publicaElPedidoCreadoConElPedidoIdComoClave() {
        when(kafkaTemplate.send(eq(TOPICO), eq(pedidoId.toString()), any(PedidoEventoKafka.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

        new PedidoKafkaPublisher(kafkaTemplate, topics).alCrearPedido(
                new PedidoCreadoEvent(pedidoId, clienteId, "Calle Falsa 456", LocalDateTime.now()));

        verify(kafkaTemplate).send(eq(TOPICO), eq(pedidoId.toString()), captor.capture());
        PedidoEventoKafka evento = (PedidoEventoKafka) captor.getValue();
        assertThat(evento.tipo()).isEqualTo("PEDIDO_CREADO");
        assertThat(evento.estado()).isEqualTo("CREADO");
        assertThat(evento.pedidoId()).isEqualTo(pedidoId);
        assertThat(evento.clienteId()).isEqualTo(clienteId);
        assertThat(evento.eventoId()).isNotBlank();
        assertThat(evento.ocurridoEn()).isNotNull();
    }

    @Test
    void publicaElCambioDeEstadoDelPedido() {
        when(kafkaTemplate.send(eq(TOPICO), eq(pedidoId.toString()), any(PedidoEventoKafka.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

        new PedidoKafkaPublisher(kafkaTemplate, topics).alCambiarEstado(
                new PedidoEstadoCambiadoEvent(pedidoId, clienteId, EstadoPedido.DESPACHADO));

        verify(kafkaTemplate).send(eq(TOPICO), eq(pedidoId.toString()), captor.capture());
        PedidoEventoKafka evento = (PedidoEventoKafka) captor.getValue();
        assertThat(evento.tipo()).isEqualTo("PEDIDO_ESTADO_CAMBIADO");
        assertThat(evento.estado()).isEqualTo("DESPACHADO");
    }

    @Test
    void unaFallaDelBrokerNoSePropaga() {
        when(kafkaTemplate.send(eq(TOPICO), eq(pedidoId.toString()), any(PedidoEventoKafka.class)))
                .thenThrow(new IllegalStateException("broker caido"));

        assertThatCode(() -> new PedidoKafkaPublisher(kafkaTemplate, topics).alCrearPedido(
                new PedidoCreadoEvent(pedidoId, clienteId, "Calle Falsa 456", LocalDateTime.now())))
                .doesNotThrowAnyException();
    }
}
