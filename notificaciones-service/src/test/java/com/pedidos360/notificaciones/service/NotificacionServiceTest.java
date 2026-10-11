package com.pedidos360.notificaciones.service;

import com.pedidos360.notificaciones.config.RabbitTopologyProperties.Canal;
import com.pedidos360.notificaciones.dto.NotificacionResponse;
import com.pedidos360.notificaciones.dto.PaginaResponse;
import com.pedidos360.notificaciones.dto.ResumenResponse;
import com.pedidos360.notificaciones.exception.SolicitudInvalidaException;
import com.pedidos360.notificaciones.model.EstadoNotificacion;
import com.pedidos360.notificaciones.model.Notificacion;
import com.pedidos360.notificaciones.model.TipoNotificacion;
import com.pedidos360.notificaciones.repository.NotificacionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    private static final Canal CANAL = new Canal("notificaciones.pedido.creado", "pedido.creado",
            "notificaciones.pedido.creado.dlq", "notificaciones.pedido.creado.dlq");

    @Mock
    private NotificacionRepository notificacionRepository;

    private Notificacion guardada() {
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void registraUnaNotificacionProcesada() {
        NotificacionService service = new NotificacionService(notificacionRepository);
        UUID referencia = UUID.randomUUID();

        service.registrarProcesada(TipoNotificacion.PEDIDO_CREADO, CANAL, "cliente-1", "Tu pedido fue creado", referencia);

        Notificacion notificacion = guardada();
        assertThat(notificacion.getTipo()).isEqualTo(TipoNotificacion.PEDIDO_CREADO);
        assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacion.PROCESADA);
        assertThat(notificacion.getRoutingKey()).isEqualTo("pedido.creado");
        assertThat(notificacion.getCola()).isEqualTo("notificaciones.pedido.creado");
        assertThat(notificacion.getDestinatario()).isEqualTo("cliente-1");
        assertThat(notificacion.getReferenciaId()).isEqualTo(referencia);
        assertThat(notificacion.getFechaRecepcion()).isNotNull();
    }

    @Test
    void limpiaSaltosDeLineaYTruncaElTextoLibre() {
        NotificacionService service = new NotificacionService(notificacionRepository);
        String largo = "x".repeat(900);

        service.registrarProcesada(TipoNotificacion.PEDIDO_CREADO, CANAL, "a".repeat(400) + "\r\nfalso",
                "linea1\r\nlinea2 " + largo, null);

        Notificacion notificacion = guardada();
        assertThat(notificacion.getMensaje()).doesNotContain("\r").doesNotContain("\n").hasSize(500);
        assertThat(notificacion.getMensaje()).startsWith("linea1 linea2");
        assertThat(notificacion.getDestinatario()).hasSize(150);
    }

    @Test
    void registraUnaNotificacionMuertaConDestinatarioSistemaYSinElCuerpo() {
        NotificacionService service = new NotificacionService(notificacionRepository);

        service.registrarMuerta(TipoNotificacion.PEDIDO_CREADO, CANAL, "rejected\ninyectado");

        Notificacion notificacion = guardada();
        assertThat(notificacion.getEstado()).isEqualTo(EstadoNotificacion.MUERTA);
        assertThat(notificacion.getDestinatario()).isEqualTo("sistema");
        assertThat(notificacion.getCola()).isEqualTo("notificaciones.pedido.creado");
        assertThat(notificacion.getMensaje()).contains("rejected inyectado").doesNotContain("\n");
        assertThat(notificacion.getReferenciaId()).isNull();
    }

    @Test
    void unaFallaDePersistenciaNoSePropaga() {
        NotificacionService service = new NotificacionService(notificacionRepository);
        when(notificacionRepository.save(any(Notificacion.class))).thenThrow(new IllegalStateException("base caida"));

        assertThatCode(() -> service.registrarProcesada(
                TipoNotificacion.PEDIDO_CREADO, CANAL, "cliente-1", "mensaje", UUID.randomUUID()))
                .doesNotThrowAnyException();
    }

    @Test
    void listaOrdenadoPorLaMasRecienteYConFiltros() {
        NotificacionService service = new NotificacionService(notificacionRepository);
        Notificacion notificacion = new Notificacion();
        notificacion.setId(UUID.randomUUID());
        notificacion.setTipo(TipoNotificacion.PEDIDO_CREADO);
        notificacion.setEstado(EstadoNotificacion.MUERTA);
        notificacion.setFechaRecepcion(Instant.now());
        Page<Notificacion> pagina = new PageImpl<>(List.of(notificacion), PageRequest.of(1, 5), 11);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        when(notificacionRepository.findByTipoAndEstado(any(), any(), captor.capture())).thenReturn(pagina);

        PaginaResponse<NotificacionResponse> respuesta =
                service.listar(TipoNotificacion.PEDIDO_CREADO, EstadoNotificacion.MUERTA, 1, 5);

        assertThat(captor.getValue().getSort().getOrderFor("fechaRecepcion").getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(respuesta.contenido()).hasSize(1);
        assertThat(respuesta.pagina()).isEqualTo(1);
        assertThat(respuesta.tamano()).isEqualTo(5);
        assertThat(respuesta.totalElementos()).isEqualTo(11);
        assertThat(respuesta.totalPaginas()).isEqualTo(3);
    }

    @Test
    void sinFiltrosListaTodoYConUnSoloFiltroUsaLaConsultaCorrespondiente() {
        NotificacionService service = new NotificacionService(notificacionRepository);
        Page<Notificacion> vacia = Page.empty();
        when(notificacionRepository.findAll(any(Pageable.class))).thenReturn(vacia);
        when(notificacionRepository.findByTipo(any(), any())).thenReturn(vacia);
        when(notificacionRepository.findByEstado(any(), any())).thenReturn(vacia);

        service.listar(null, null, 0, 20);
        service.listar(TipoNotificacion.PEDIDO_CREADO, null, 0, 20);
        service.listar(null, EstadoNotificacion.PROCESADA, 0, 20);

        verify(notificacionRepository).findAll(any(Pageable.class));
        verify(notificacionRepository).findByTipo(any(), any());
        verify(notificacionRepository).findByEstado(any(), any());
        verify(notificacionRepository, never()).findByTipoAndEstado(any(), any(), any());
    }

    @Test
    void rechazaParametrosDePaginacionInvalidos() {
        NotificacionService service = new NotificacionService(notificacionRepository);

        assertThatThrownBy(() -> service.listar(null, null, -1, 20)).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> service.listar(null, null, 0, 0)).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> service.listar(null, null, 0, 101)).isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void elResumenIncluyeTodasLasClavesAunqueSeanCero() {
        NotificacionService service = new NotificacionService(notificacionRepository);
        when(notificacionRepository.count()).thenReturn(7L);
        when(notificacionRepository.countByTipo(any())).thenReturn(0L);
        when(notificacionRepository.countByEstado(any())).thenReturn(0L);
        when(notificacionRepository.countByTipo(TipoNotificacion.PEDIDO_CREADO)).thenReturn(4L);
        when(notificacionRepository.countByEstado(EstadoNotificacion.MUERTA)).thenReturn(2L);

        ResumenResponse resumen = service.resumir();

        assertThat(resumen.total()).isEqualTo(7);
        assertThat(resumen.porTipo()).containsOnlyKeys("PEDIDO_CREADO").containsEntry("PEDIDO_CREADO", 4L);
        assertThat(resumen.porEstado()).containsOnlyKeys("PROCESADA", "MUERTA")
                .containsEntry("MUERTA", 2L).containsEntry("PROCESADA", 0L);
    }
}
