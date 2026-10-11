package com.pedidos360.envios.service;

import com.pedidos360.envios.dto.CambioEstadoEnvioDTO;
import com.pedidos360.envios.dto.EnvioRequestDTO;
import com.pedidos360.envios.dto.EnvioResponseDTO;
import com.pedidos360.envios.event.EnvioAsignadoEvent;
import com.pedidos360.envios.event.EnvioEntregadoEvent;
import com.pedidos360.envios.event.EnvioEstadoCambiadoEvent;
import com.pedidos360.envios.model.Envio;
import com.pedidos360.envios.model.EstadoEnvio;
import com.pedidos360.envios.model.EventoEnvio;
import com.pedidos360.envios.repository.EnvioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EnvioService {

    private final EnvioRepository envioRepository;

    private final ApplicationEventPublisher eventPublisher;

    public EnvioService(EnvioRepository envioRepository, ApplicationEventPublisher eventPublisher) {
        this.envioRepository = envioRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public EnvioResponseDTO crear(EnvioRequestDTO request) {
        Envio envio = new Envio();
        envio.setPedidoId(request.pedidoId());
        envio.setRepartidorId(request.repartidorId());
        envio.setFechaAsignacion(LocalDateTime.now());
        envio.setFechaEntregaEstimada(request.fechaEntregaEstimada());

        EventoEnvio evento = new EventoEnvio();
        evento.setTipoEvento("ASIGNACION");
        evento.setDescripcion("Envio creado y asignado al repartidor " + request.repartidorId());
        envio.registrarEvento(evento);

        EnvioResponseDTO creado = EnvioResponseDTO.from(envioRepository.save(envio));
        eventPublisher.publishEvent(new EnvioAsignadoEvent(
                creado.id(), creado.pedidoId(), creado.repartidorId(), creado.fechaEntregaEstimada()));
        return creado;
    }

    @Transactional(readOnly = true)
    public EnvioResponseDTO buscarPorId(UUID id) {
        Envio envio = envioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Envio no encontrado: " + id));
        return EnvioResponseDTO.from(envio);
    }

    @Transactional
    public EnvioResponseDTO cambiarEstado(UUID id, CambioEstadoEnvioDTO cambio) {
        Envio envio = envioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Envio no encontrado: " + id));

        envio.setEstadoEnvio(cambio.estadoEnvio());

        EventoEnvio evento = new EventoEnvio();
        evento.setTipoEvento("CAMBIO_ESTADO");
        evento.setDescripcion(cambio.descripcionEvento());
        envio.registrarEvento(evento);

        EnvioResponseDTO actualizado = EnvioResponseDTO.from(envioRepository.save(envio));
        publicarCambioDeEstado(actualizado, cambio);
        return actualizado;
    }

    private void publicarCambioDeEstado(EnvioResponseDTO envio, CambioEstadoEnvioDTO cambio) {
        LocalDateTime ahora = LocalDateTime.now();
        eventPublisher.publishEvent(new EnvioEstadoCambiadoEvent(
                envio.id(), envio.pedidoId(), envio.estadoEnvio(), cambio.descripcionEvento(), ahora));
        if (envio.estadoEnvio() == EstadoEnvio.ENTREGADO) {
            eventPublisher.publishEvent(new EnvioEntregadoEvent(
                    envio.id(), envio.pedidoId(), envio.repartidorId(), ahora));
        }
    }

    @Transactional(readOnly = true)
    public List<EnvioResponseDTO> buscarPorRepartidor(UUID repartidorId) {
        return envioRepository.findByRepartidorId(repartidorId).stream()
                .map(EnvioResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EnvioResponseDTO buscarPorPedido(UUID pedidoId) {
        Envio envio = envioRepository.findByPedidoId(pedidoId)
                .orElseThrow(() -> new EntityNotFoundException("Envio no encontrado para pedido: " + pedidoId));
        return EnvioResponseDTO.from(envio);
    }
}
