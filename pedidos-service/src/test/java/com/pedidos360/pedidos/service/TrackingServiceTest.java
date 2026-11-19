package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.consumer.kafka.EnvioTrackingEvento;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TrackingServiceTest {

    private final TrackingService service = new TrackingService();
    private final UUID pedidoId = UUID.randomUUID();
    private final Instant base = Instant.parse("2026-01-01T10:00:00Z");

    private EnvioTrackingEvento evento(String estado, Instant ocurridoEn) {
        return new EnvioTrackingEvento(UUID.randomUUID().toString(), UUID.randomUUID(), pedidoId, UUID.randomUUID(), estado, ocurridoEn);
    }

    @Test
    void guardaElUltimoEstadoPorPedido() {
        service.registrar(evento("PENDIENTE", base));
        service.registrar(evento("EN_TRANSITO", base.plusSeconds(60)));

        assertThat(service.buscar(pedidoId)).get().extracting(EnvioTrackingEvento::estado).isEqualTo("EN_TRANSITO");
    }

    @Test
    void ignoraUnEventoMasAntiguoQueElGuardado() {
        service.registrar(evento("EN_TRANSITO", base.plusSeconds(60)));
        service.registrar(evento("PENDIENTE", base));

        assertThat(service.buscar(pedidoId)).get().extracting(EnvioTrackingEvento::estado).isEqualTo("EN_TRANSITO");
    }

    @Test
    void devuelveVacioParaUnPedidoSinTracking() {
        assertThat(service.buscar(UUID.randomUUID())).isEmpty();
    }
}
