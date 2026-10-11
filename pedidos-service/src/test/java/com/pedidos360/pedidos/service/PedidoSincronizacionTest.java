package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.model.EstadoPedido;
import com.pedidos360.pedidos.model.Pedido;
import com.pedidos360.pedidos.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoSincronizacionTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PedidoService service() {
        return new PedidoService(pedidoRepository, eventPublisher);
    }

    private Pedido pedidoEn(EstadoPedido estado) {
        Pedido pedido = new Pedido();
        pedido.setEstado(estado);
        when(pedidoRepository.findByIdForUpdate(any())).thenReturn(Optional.of(pedido));
        return pedido;
    }

    @ParameterizedTest
    @CsvSource({
            "EN_TRANSITO, DESPACHADO",
            "ENTREGADO, ENTREGADO"
    })
    void mapeaElEstadoDelEnvioAlEstadoDelPedido(String estadoEnvio, EstadoPedido esperado) {
        assertThat(PedidoService.estadoPedidoPara(estadoEnvio)).contains(esperado);
    }

    @Test
    void pendienteYFallidoNoMapeanAningunEstado() {
        assertThat(PedidoService.estadoPedidoPara("PENDIENTE")).isEmpty();
        assertThat(PedidoService.estadoPedidoPara("FALLIDO")).isEmpty();
        assertThat(PedidoService.estadoPedidoPara("OTRO")).isEmpty();
    }

    @Test
    void envioAsignadoDespachaUnPedidoCreado() {
        Pedido pedido = pedidoEn(EstadoPedido.CREADO);

        service().sincronizarConEnvioAsignado(UUID.randomUUID());

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.DESPACHADO);
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void enTransitoDespachaYEntregadoEntrega() {
        Pedido creado = pedidoEn(EstadoPedido.CREADO);
        service().sincronizarConEstadoDeEnvio(UUID.randomUUID(), "EN_TRANSITO");
        assertThat(creado.getEstado()).isEqualTo(EstadoPedido.DESPACHADO);

        Pedido despachado = pedidoEn(EstadoPedido.DESPACHADO);
        service().sincronizarConEstadoDeEnvio(UUID.randomUUID(), "ENTREGADO");
        assertThat(despachado.getEstado()).isEqualTo(EstadoPedido.ENTREGADO);
    }

    @Test
    void esIdempotenteSiElPedidoYaEstaEnElEstadoDestino() {
        Pedido pedido = pedidoEn(EstadoPedido.DESPACHADO);

        service().sincronizarConEnvioAsignado(UUID.randomUUID());
        service().sincronizarConEstadoDeEnvio(UUID.randomUUID(), "EN_TRANSITO");

        assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.DESPACHADO);
        verify(pedidoRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({
            "ENTREGADO, EN_TRANSITO",
            "CANCELADO, EN_TRANSITO",
            "CANCELADO, ENTREGADO",
            "ENTREGADO, ENTREGADO"
    })
    void nuncaRetrocedeNiReviveUnPedidoTerminal(EstadoPedido actual, String estadoEnvio) {
        Pedido pedido = pedidoEn(actual);

        service().sincronizarConEstadoDeEnvio(UUID.randomUUID(), estadoEnvio);

        assertThat(pedido.getEstado()).isEqualTo(actual);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void envioAsignadoNoMueveUnPedidoEntregadoOCancelado() {
        Pedido entregado = pedidoEn(EstadoPedido.ENTREGADO);
        service().sincronizarConEnvioAsignado(UUID.randomUUID());
        assertThat(entregado.getEstado()).isEqualTo(EstadoPedido.ENTREGADO);

        Pedido cancelado = pedidoEn(EstadoPedido.CANCELADO);
        service().sincronizarConEnvioAsignado(UUID.randomUUID());
        assertThat(cancelado.getEstado()).isEqualTo(EstadoPedido.CANCELADO);

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void ignoraPendienteYFallidoSinTocarElPedido() {
        service().sincronizarConEstadoDeEnvio(UUID.randomUUID(), "PENDIENTE");
        service().sincronizarConEstadoDeEnvio(UUID.randomUUID(), "FALLIDO");

        verify(pedidoRepository, never()).findByIdForUpdate(any());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void ignoraUnPedidoDesconocidoSinLanzarExcepcion() {
        when(pedidoRepository.findByIdForUpdate(any())).thenReturn(Optional.empty());

        service().sincronizarConEnvioAsignado(UUID.randomUUID());
        service().sincronizarConEstadoDeEnvio(UUID.randomUUID(), "ENTREGADO");

        verify(pedidoRepository, never()).save(any());
    }
}
