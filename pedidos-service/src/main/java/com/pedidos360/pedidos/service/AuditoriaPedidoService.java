package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.consumer.kafka.PedidoEventoKafka;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AuditoriaPedidoService {

    private final Map<UUID, List<PedidoEventoKafka>> historialPorPedido = new ConcurrentHashMap<>();

    public void registrar(PedidoEventoKafka evento) {
        List<PedidoEventoKafka> historial = historialPorPedido.computeIfAbsent(evento.pedidoId(), id -> new CopyOnWriteArrayList<>());
        boolean repetido = historial.stream().anyMatch(existente -> existente.eventoId().equals(evento.eventoId()));
        if (!repetido) {
            historial.add(evento);
        }
    }

    public List<PedidoEventoKafka> historial(UUID pedidoId) {
        return List.copyOf(historialPorPedido.getOrDefault(pedidoId, List.of()));
    }
}
