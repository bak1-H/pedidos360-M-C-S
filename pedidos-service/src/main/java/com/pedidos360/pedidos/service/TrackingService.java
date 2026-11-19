package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.consumer.kafka.EnvioTrackingEvento;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TrackingService {

    private final Map<UUID, EnvioTrackingEvento> ultimoPorPedido = new ConcurrentHashMap<>();

    public void registrar(EnvioTrackingEvento evento) {
        ultimoPorPedido.merge(evento.pedidoId(), evento,
                (actual, nuevo) -> nuevo.ocurridoEn().isBefore(actual.ocurridoEn()) ? actual : nuevo);
    }

    public Optional<EnvioTrackingEvento> buscar(UUID pedidoId) {
        return Optional.ofNullable(ultimoPorPedido.get(pedidoId));
    }
}
