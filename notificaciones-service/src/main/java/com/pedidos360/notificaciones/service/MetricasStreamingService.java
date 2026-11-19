package com.pedidos360.notificaciones.service;

import com.pedidos360.notificaciones.consumer.kafka.EnvioTrackingEvento;
import com.pedidos360.notificaciones.consumer.kafka.PedidoEventoKafka;
import com.pedidos360.notificaciones.dto.EventoRecienteResponse;
import com.pedidos360.notificaciones.dto.MetricasStreamingResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Service
public class MetricasStreamingService {

    static final int MAXIMO_EVENTOS_RECIENTES = 20;

    private final Map<String, LongAdder> pedidosPorEstado = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> pedidosPorTipo = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> enviosPorEstado = new ConcurrentHashMap<>();
    private final Deque<EventoRecienteResponse> recientes = new ArrayDeque<>();

    public void registrarPedido(PedidoEventoKafka evento) {
        incrementar(pedidosPorEstado, evento.estado());
        incrementar(pedidosPorTipo, evento.tipo());
        recordar(new EventoRecienteResponse("PEDIDO", evento.eventoId(), evento.tipo(),
                evento.pedidoId(), evento.estado(), evento.ocurridoEn()));
    }

    public void registrarEnvio(EnvioTrackingEvento evento) {
        incrementar(enviosPorEstado, evento.estado());
        recordar(new EventoRecienteResponse("ENVIO", evento.eventoId(), "ENVIO_TRACKING",
                evento.pedidoId(), evento.estado(), evento.ocurridoEn()));
    }

    public MetricasStreamingResponse instantanea() {
        Map<String, Long> porEstadoPedido = copiar(pedidosPorEstado);
        Map<String, Long> porEstadoEnvio = copiar(enviosPorEstado);
        return new MetricasStreamingResponse(
                total(porEstadoPedido),
                total(porEstadoEnvio),
                porEstadoPedido,
                copiar(pedidosPorTipo),
                porEstadoEnvio,
                ultimos());
    }

    private void incrementar(Map<String, LongAdder> contadores, String clave) {
        contadores.computeIfAbsent(clave, k -> new LongAdder()).increment();
    }

    private synchronized void recordar(EventoRecienteResponse evento) {
        recientes.addFirst(evento);
        if (recientes.size() > MAXIMO_EVENTOS_RECIENTES) {
            recientes.removeLast();
        }
    }

    private synchronized List<EventoRecienteResponse> ultimos() {
        return List.copyOf(recientes);
    }

    private Map<String, Long> copiar(Map<String, LongAdder> contadores) {
        Map<String, Long> copia = new TreeMap<>();
        contadores.forEach((clave, contador) -> copia.put(clave, contador.sum()));
        return copia;
    }

    private long total(Map<String, Long> contadores) {
        return contadores.values().stream().mapToLong(Long::longValue).sum();
    }
}
