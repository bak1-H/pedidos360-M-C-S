package com.pedidos360.notificaciones.model;

import jakarta.persistence.AttributeConverter;

public class TipoNotificacionConverter implements AttributeConverter<TipoNotificacion, String> {

    @Override
    public String convertToDatabaseColumn(TipoNotificacion tipo) {
        return tipo == null ? null : tipo.name();
    }

    @Override
    public TipoNotificacion convertToEntityAttribute(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return TipoNotificacion.valueOf(valor);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
