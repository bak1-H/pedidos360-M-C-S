package com.pedidos360.notificaciones.repository;

import com.pedidos360.notificaciones.model.EstadoNotificacion;
import com.pedidos360.notificaciones.model.Notificacion;
import com.pedidos360.notificaciones.model.TipoNotificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {

    Page<Notificacion> findByTipo(TipoNotificacion tipo, Pageable pageable);

    Page<Notificacion> findByEstado(EstadoNotificacion estado, Pageable pageable);

    Page<Notificacion> findByTipoAndEstado(TipoNotificacion tipo, EstadoNotificacion estado, Pageable pageable);

    long countByTipo(TipoNotificacion tipo);

    long countByEstado(EstadoNotificacion estado);
}
