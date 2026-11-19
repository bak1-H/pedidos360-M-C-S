package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.consumer.kafka.PedidoEventoKafka;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditoriaPedidoServiceTest {

    private final AuditoriaPedidoService service = new AuditoriaPedidoService();
    private final UUID pedidoId = UUID.randomUUID();

    private PedidoEventoKafka evento(String eventoId, String estado) {
        return new PedidoEventoKafka(eventoId, "PEDIDO_ESTADO_CAMBIADO", pedidoId, UUID.randomUUID(), estado, Instant.now());
    }

    @Test
    void conservaElHistorialDelPedidoEnOrdenDeLlegada() {
        service.registrar(evento("e1", "CREADO"));
        service.registrar(evento("e2", "DESPACHADO"));

        assertThat(service.historial(pedidoId)).extracting(PedidoEventoKafka::estado).containsExactly("CREADO", "DESPACHADO");
    }

    @Test
    void ignoraUnEventoEntregadoDosVeces() {
        service.registrar(evento("e1", "CREADO"));
        service.registrar(evento("e1", "CREADO"));

        assertThat(service.historial(pedidoId)).hasSize(1);
    }

    @Test
    void devuelveVacioParaUnPedidoSinEventos() {
        assertThat(service.historial(UUID.randomUUID())).isEmpty();
    }
}
