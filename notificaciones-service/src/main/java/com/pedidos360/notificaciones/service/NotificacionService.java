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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificacionService {

    public static final int TAMANO_MAXIMO = 100;

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private static final String DESTINATARIO_SISTEMA = "sistema";

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    public void registrarProcesada(TipoNotificacion tipo, Canal canal, String destinatario,
                                   String mensaje, UUID referenciaId) {
        registrar(tipo, EstadoNotificacion.PROCESADA, canal, destinatario, mensaje, referenciaId);
    }

    public void registrarMuerta(TipoNotificacion tipo, Canal canal, String motivo) {
        String mensaje = "Mensaje muerto en la DLQ, motivo: " + Sanitizador.limpiar(motivo, 100);
        registrar(tipo, EstadoNotificacion.MUERTA, canal, DESTINATARIO_SISTEMA, mensaje, null);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<NotificacionResponse> listar(TipoNotificacion tipo, EstadoNotificacion estado,
                                                       int pagina, int tamano) {
        if (pagina < 0) {
            throw new SolicitudInvalidaException("página no puede ser negativa");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new SolicitudInvalidaException("tamaño debe estar entre 1 y " + TAMANO_MAXIMO);
        }
        Pageable pageable = PageRequest.of(pagina, tamano, Sort.by(Sort.Direction.DESC, "fechaRecepcion").and(Sort.by("id")));
        Page<Notificacion> resultado = buscar(tipo, estado, pageable);
        return new PaginaResponse<>(
                resultado.getContent().stream().map(NotificacionResponse::from).toList(),
                pagina,
                tamano,
                resultado.getTotalElements(),
                resultado.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ResumenResponse resumir() {
        Map<String, Long> porTipo = new LinkedHashMap<>();
        for (TipoNotificacion tipo : TipoNotificacion.values()) {
            porTipo.put(tipo.name(), notificacionRepository.countByTipo(tipo));
        }
        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (EstadoNotificacion estado : EstadoNotificacion.values()) {
            porEstado.put(estado.name(), notificacionRepository.countByEstado(estado));
        }
        return new ResumenResponse(notificacionRepository.count(), porTipo, porEstado);
    }

    private Page<Notificacion> buscar(TipoNotificacion tipo, EstadoNotificacion estado, Pageable pageable) {
        if (tipo != null && estado != null) {
            return notificacionRepository.findByTipoAndEstado(tipo, estado, pageable);
        }
        if (tipo != null) {
            return notificacionRepository.findByTipo(tipo, pageable);
        }
        if (estado != null) {
            return notificacionRepository.findByEstado(estado, pageable);
        }
        return notificacionRepository.findAll(pageable);
    }

    private void registrar(TipoNotificacion tipo, EstadoNotificacion estado, Canal canal,
                           String destinatario, String mensaje, UUID referenciaId) {
        try {
            Notificacion notificacion = new Notificacion();
            notificacion.setTipo(tipo);
            notificacion.setEstado(estado);
            notificacion.setRoutingKey(canal.routingKey());
            notificacion.setCola(canal.queue());
            notificacion.setDestinatario(Sanitizador.limpiar(destinatario, Notificacion.LONGITUD_DESTINATARIO));
            notificacion.setMensaje(Sanitizador.limpiar(mensaje, Notificacion.LONGITUD_MENSAJE));
            notificacion.setReferenciaId(referenciaId);
            notificacion.setFechaRecepcion(Instant.now());
            notificacionRepository.save(notificacion);
        } catch (RuntimeException ex) {
            log.error("No se pudo registrar la notificacion tipo={} estado={} cola={} referenciaId={}: {}",
                    tipo, estado, canal.queue(), referenciaId, ex.getClass().getName());
        }
    }
}
