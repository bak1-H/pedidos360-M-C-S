package com.pedidos360.pedidos.consumer.kafka;

import com.pedidos360.pedidos.consumer.PayloadInvalidoException;
import com.pedidos360.pedidos.service.AuditoriaPedidoService;
import jakarta.validation.Validation;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AuditoriaPedidoConsumerTest {

    private final AuditoriaPedidoService auditoria = mock(AuditoriaPedidoService.class);
    private final AuditoriaPedidoConsumer consumer = new AuditoriaPedidoConsumer(
            new ValidadorPayload(Validation.buildDefaultValidatorFactory().getValidator()), auditoria);

    private ConsumerRecord<String, PedidoEventoKafka> registro(PedidoEventoKafka evento) {
        return new ConsumerRecord<>("pedidos360.pedidos.eventos", 0, 0L, "clave", evento);
    }

    @Test
    void delegaElEventoValidoEnElServicioDeAuditoria() {
        PedidoEventoKafka evento = new PedidoEventoKafka(
                "e1", "PEDIDO_CREADO", UUID.randomUUID(), UUID.randomUUID(), "CREADO", Instant.now());

        consumer.alRecibirEventoDePedido(registro(evento));

        verify(auditoria).registrar(evento);
    }

    @Test
    void rechazaUnEventoSinCamposObligatorios() {
        PedidoEventoKafka invalido = new PedidoEventoKafka("", "PEDIDO_CREADO", null, UUID.randomUUID(), "CREADO", null);

        assertThatThrownBy(() -> consumer.alRecibirEventoDePedido(registro(invalido)))
                .isInstanceOf(PayloadInvalidoException.class);
        verifyNoInteractions(auditoria);
    }

    @Test
    void deserializaUnEventoEnriquecidoIgnorandoLosCamposQueNoDeclara() {
        String json = """
                {"eventoId":"e1","tipo":"PEDIDO_CREADO","pedidoId":"%s","clienteId":"%s","estado":"CREADO",
                 "ocurridoEn":"2026-01-01T10:00:00Z","direccionDestino":"Calle Falsa 456","cantidadItems":3,
                 "items":[{"descripcion":"Teclado","cantidad":2,"valorDeclarado":15000}]}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        try (JsonDeserializer<PedidoEventoKafka> deserializador = new JsonDeserializer<>(PedidoEventoKafka.class, false)) {
            PedidoEventoKafka evento = deserializador.deserialize("topico", json.getBytes(StandardCharsets.UTF_8));

            assertThat(evento.estado()).isEqualTo("CREADO");
        }
    }
}
