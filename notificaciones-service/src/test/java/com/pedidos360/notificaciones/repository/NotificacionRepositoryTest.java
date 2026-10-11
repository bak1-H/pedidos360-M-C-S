package com.pedidos360.notificaciones.repository;

import com.pedidos360.notificaciones.model.EstadoNotificacion;
import com.pedidos360.notificaciones.model.Notificacion;
import com.pedidos360.notificaciones.model.TipoNotificacion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NotificacionRepositoryTest {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void guardar(TipoNotificacion tipo, EstadoNotificacion estado, Instant fecha) {
        Notificacion notificacion = new Notificacion();
        notificacion.setTipo(tipo);
        notificacion.setEstado(estado);
        notificacion.setRoutingKey("rk");
        notificacion.setCola("q");
        notificacion.setDestinatario("destinatario");
        notificacion.setMensaje("mensaje");
        notificacion.setFechaRecepcion(fecha);
        notificacionRepository.saveAndFlush(notificacion);
    }

    @Test
    void unTipoLegadoDesconocidoNoRompeLaLecturaYSeLeeComoNulo() {
        jdbcTemplate.update(
                "insert into notificacion (id, tipo, estado, routing_key, cola, destinatario, mensaje, fecha_recepcion)"
                        + " values (?, 'TIPO_RETIRADO', 'PROCESADA', 'rk', 'q', 'd', 'm', ?)",
                UUID.randomUUID(), java.sql.Timestamp.from(Instant.parse("2026-10-10T10:00:00Z")));

        Page<Notificacion> pagina = notificacionRepository.findAll(PageRequest.of(0, 10));

        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().get(0).getTipo()).isNull();
    }

    @Test
    void filtraYPaginaDeLaMasRecienteALaMasAntigua() {
        Instant base = Instant.parse("2026-10-10T10:00:00Z");
        guardar(TipoNotificacion.PEDIDO_CREADO, EstadoNotificacion.PROCESADA, base);
        guardar(TipoNotificacion.PEDIDO_CREADO, EstadoNotificacion.MUERTA, base.plusSeconds(10));
        guardar(TipoNotificacion.PEDIDO_CREADO, EstadoNotificacion.PROCESADA, base.plusSeconds(20));
        PageRequest primeraPagina = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "fechaRecepcion"));

        Page<Notificacion> todas = notificacionRepository.findAll(primeraPagina);
        Page<Notificacion> pedidos = notificacionRepository.findByTipo(TipoNotificacion.PEDIDO_CREADO, primeraPagina);
        Page<Notificacion> muertas = notificacionRepository.findByTipoAndEstado(
                TipoNotificacion.PEDIDO_CREADO, EstadoNotificacion.MUERTA, primeraPagina);

        assertThat(todas.getTotalElements()).isEqualTo(3);
        assertThat(todas.getContent()).extracting(Notificacion::getEstado)
                .containsExactly(EstadoNotificacion.PROCESADA, EstadoNotificacion.MUERTA);
        assertThat(pedidos.getContent()).extracting(Notificacion::getEstado)
                .containsExactly(EstadoNotificacion.PROCESADA, EstadoNotificacion.MUERTA);
        assertThat(muertas.getTotalElements()).isEqualTo(1);
        assertThat(notificacionRepository.countByEstado(EstadoNotificacion.PROCESADA)).isEqualTo(2);
        assertThat(notificacionRepository.countByTipo(TipoNotificacion.PEDIDO_CREADO)).isEqualTo(3);
    }
}
