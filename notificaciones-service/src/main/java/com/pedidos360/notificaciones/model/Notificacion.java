package com.pedidos360.notificaciones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notificacion")
public class Notificacion {

    public static final int LONGITUD_DESTINATARIO = 150;
    public static final int LONGITUD_MENSAJE = 500;

    @Id
    @GeneratedValue
    private UUID id;

    @Convert(converter = TipoNotificacionConverter.class)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoNotificacion tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoNotificacion estado;

    @Column(name = "routing_key", nullable = false, length = 150)
    private String routingKey;

    @Column(name = "cola", nullable = false, length = 150)
    private String cola;

    @Column(name = "destinatario", nullable = false, length = LONGITUD_DESTINATARIO)
    private String destinatario;

    @Column(name = "mensaje", nullable = false, length = LONGITUD_MENSAJE)
    private String mensaje;

    @Column(name = "referencia_id")
    private UUID referenciaId;

    @Column(name = "fecha_recepcion", nullable = false)
    private Instant fechaRecepcion;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TipoNotificacion getTipo() {
        return tipo;
    }

    public void setTipo(TipoNotificacion tipo) {
        this.tipo = tipo;
    }

    public EstadoNotificacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoNotificacion estado) {
        this.estado = estado;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getCola() {
        return cola;
    }

    public void setCola(String cola) {
        this.cola = cola;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public UUID getReferenciaId() {
        return referenciaId;
    }

    public void setReferenciaId(UUID referenciaId) {
        this.referenciaId = referenciaId;
    }

    public Instant getFechaRecepcion() {
        return fechaRecepcion;
    }

    public void setFechaRecepcion(Instant fechaRecepcion) {
        this.fechaRecepcion = fechaRecepcion;
    }
}
