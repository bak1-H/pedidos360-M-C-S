package com.pedidos360.envios.service;

import com.pedidos360.envios.dto.CambioEstadoEnvioDTO;
import com.pedidos360.envios.dto.EnvioRequestDTO;
import com.pedidos360.envios.dto.EnvioResponseDTO;
import com.pedidos360.envios.model.Envio;
import com.pedidos360.envios.model.EventoEnvio;
import com.pedidos360.envios.repository.EnvioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EnvioService {

    private final EnvioRepository envioRepository;

    public EnvioService(EnvioRepository envioRepository) {
        this.envioRepository = envioRepository;
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

        return EnvioResponseDTO.from(envioRepository.save(envio));
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

        return EnvioResponseDTO.from(envioRepository.save(envio));
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
