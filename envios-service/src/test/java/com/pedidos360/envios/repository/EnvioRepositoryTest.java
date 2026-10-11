package com.pedidos360.envios.repository;

import com.pedidos360.envios.model.Envio;
import com.pedidos360.envios.model.EventoEnvio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EnvioRepositoryTest {

    @Autowired
    private EnvioRepository envioRepository;

    @Test
    void guardaEnvioConEventoYLoRecupera() {
        UUID pedidoId = UUID.randomUUID();
        UUID repartidorId = UUID.randomUUID();

        Envio envio = new Envio();
        envio.setPedidoId(pedidoId);
        envio.setRepartidorId(repartidorId);

        EventoEnvio evento = new EventoEnvio();
        evento.setTipoEvento("ASIGNACION");
        evento.setDescripcion("Envio creado");
        envio.registrarEvento(evento);

        Envio guardado = envioRepository.saveAndFlush(envio);

        Optional<Envio> recuperado = envioRepository.findById(guardado.getId());
        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getEventos()).hasSize(1);
    }

    @Test
    void encuentraPorRepartidorIdYPorPedidoId() {
        UUID pedidoId = UUID.randomUUID();
        UUID repartidorId = UUID.randomUUID();

        Envio envio = new Envio();
        envio.setPedidoId(pedidoId);
        envio.setRepartidorId(repartidorId);
        envioRepository.saveAndFlush(envio);

        List<Envio> porRepartidor = envioRepository.findByRepartidorId(repartidorId);
        Optional<Envio> porPedido = envioRepository.findByPedidoId(pedidoId);

        assertThat(porRepartidor).hasSize(1);
        assertThat(porPedido).isPresent();
    }

    @Test
    void unPedidoRepetidoLanzaUnaViolacionQueSeReconoceComoDeUnicidad() {
        UUID pedidoId = UUID.randomUUID();
        Envio primero = new Envio();
        primero.setPedidoId(pedidoId);
        Envio segundo = new Envio();
        segundo.setPedidoId(pedidoId);
        envioRepository.saveAndFlush(primero);

        Throwable falla = org.assertj.core.api.Assertions.catchThrowable(() -> envioRepository.saveAndFlush(segundo));

        assertThat(falla).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(com.pedidos360.envios.exception.ViolacionUnicidad.esDeUnicidad(falla)).isTrue();
    }

    @Test
    void existePorPedidoIdYListaMasRecientesPrimero() {
        UUID pedidoViejo = UUID.randomUUID();
        UUID pedidoNuevo = UUID.randomUUID();
        Envio viejo = new Envio();
        viejo.setPedidoId(pedidoViejo);
        viejo.setFechaAsignacion(LocalDateTime.now().minusDays(1));
        Envio nuevo = new Envio();
        nuevo.setPedidoId(pedidoNuevo);
        nuevo.setFechaAsignacion(LocalDateTime.now());
        envioRepository.saveAndFlush(viejo);
        envioRepository.saveAndFlush(nuevo);

        assertThat(envioRepository.existsByPedidoId(pedidoViejo)).isTrue();
        assertThat(envioRepository.existsByPedidoId(UUID.randomUUID())).isFalse();
        assertThat(envioRepository.findAllByOrderByFechaAsignacionDesc())
                .extracting(Envio::getPedidoId).containsExactly(pedidoNuevo, pedidoViejo);
    }
}
