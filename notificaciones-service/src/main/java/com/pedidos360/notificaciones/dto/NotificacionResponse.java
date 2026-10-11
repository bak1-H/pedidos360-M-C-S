package com.pedidos360.notificaciones.dto;

import com.pedidos360.notificaciones.model.EstadoNotificacion;
import com.pedidos360.notificaciones.model.Notificacion;
import com.pedidos360.notificaciones.model.TipoNotificacion;

import java.time.Instant;
import java.util.UUID;

public record NotificacionResponse(
        UUID id,
        TipoNotificacion tipo,
        EstadoNotificacion estado,
        String routingKey,
        String cola,
        String destinatario,
        String mensaje,
        UUID referenciaId,
        Instant fechaRecepcion
) {

    public static NotificacionResponse from(Notificacion notificacion) {
        return new NotificacionResponse(
                notificacion.getId(),
                notificacion.getTipo(),
                notificacion.getEstado(),
                notificacion.getRoutingKey(),
                notificacion.getCola(),
                notificacion.getDestinatario(),
                notificacion.getMensaje(),
                notificacion.getReferenciaId(),
                notificacion.getFechaRecepcion()
        );
    }
}
