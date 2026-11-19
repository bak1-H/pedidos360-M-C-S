package com.pedidos360.notificaciones.service;

import com.pedidos360.notificaciones.consumer.kafka.EnvioTrackingEvento;
import com.pedidos360.notificaciones.consumer.kafka.PedidoEventoKafka;
import com.pedidos360.notificaciones.dto.MetricasStreamingResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class MetricasStreamingServiceTest {

    private final MetricasStreamingService service = new MetricasStreamingService();

    private PedidoEventoKafka pedido(String tipo, String estado) {
        return new PedidoEventoKafka(UUID.randomUUID().toString(), tipo, UUID.randomUUID(), UUID.randomUUID(), estado, Instant.now());
    }

    private EnvioTrackingEvento envio(String estado) {
        return new EnvioTrackingEvento(UUID.randomUUID().toString(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), estado, Instant.now());
    }

    @Test
    void cuentaPedidosPorEstadoYTipoYEnviosPorEstado() {
        service.registrarPedido(pedido("PEDIDO_CREADO", "CREADO"));
        service.registrarPedido(pedido("PEDIDO_CREADO", "CREADO"));
        service.registrarPedido(pedido("PEDIDO_ESTADO_CAMBIADO", "ENTREGADO"));
        service.registrarEnvio(envio("EN_TRANSITO"));

        MetricasStreamingResponse metricas = service.instantanea();

        assertThat(metricas.pedidosPorEstado()).containsEntry("CREADO", 2L).containsEntry("ENTREGADO", 1L);
        assertThat(metricas.pedidosPorTipo()).containsEntry("PEDIDO_CREADO", 2L).containsEntry("PEDIDO_ESTADO_CAMBIADO", 1L);
        assertThat(metricas.enviosPorEstado()).containsEntry("EN_TRANSITO", 1L);
        assertThat(metricas.totalEventosPedido()).isEqualTo(3L);
        assertThat(metricas.totalEventosEnvio()).isEqualTo(1L);
    }

    @Test
    void conservaSoloLosUltimosEventosMasRecientesPrimero() {
        IntStream.range(0, MetricasStreamingService.MAXIMO_EVENTOS_RECIENTES + 5)
                .forEach(i -> service.registrarPedido(pedido("PEDIDO_CREADO", "CREADO")));
        service.registrarEnvio(envio("ENTREGADO"));

        MetricasStreamingResponse metricas = service.instantanea();

        assertThat(metricas.ultimosEventos()).hasSize(MetricasStreamingService.MAXIMO_EVENTOS_RECIENTES);
        assertThat(metricas.ultimosEventos().get(0).origen()).isEqualTo("ENVIO");
    }

    @Test
    void soportaRegistrosConcurrentes() throws InterruptedException {
        ExecutorService ejecutor = Executors.newFixedThreadPool(8);
        IntStream.range(0, 1000).forEach(i -> ejecutor.submit(() -> service.registrarPedido(pedido("PEDIDO_CREADO", "CREADO"))));
        ejecutor.shutdown();
        assertThat(ejecutor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(service.instantanea().pedidosPorEstado()).containsEntry("CREADO", 1000L);
    }
}
