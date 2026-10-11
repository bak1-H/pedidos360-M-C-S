package com.pedidos360.notificaciones.service;

import com.pedidos360.notificaciones.config.RabbitTopologyProperties;
import com.pedidos360.notificaciones.config.RabbitTopologyProperties.Canal;
import com.pedidos360.notificaciones.dto.CategoriaCola;
import com.pedidos360.notificaciones.dto.ColaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ColaService {

    private static final Logger log = LoggerFactory.getLogger(ColaService.class);

    private final AmqpAdmin rabbitAdmin;
    private final RabbitTopologyProperties topology;

    public ColaService(AmqpAdmin rabbitAdmin, RabbitTopologyProperties topology) {
        this.rabbitAdmin = rabbitAdmin;
        this.topology = topology;
    }

    public List<ColaResponse> listar() {
        List<ColaResponse> colas = new ArrayList<>();
        Canal canal = topology.pedidoCreado();
        agregar(colas, CategoriaCola.NEGOCIO, canal.queue());
        agregar(colas, CategoriaCola.DLQ, canal.deadLetterQueue());
        return colas;
    }

    private void agregar(List<ColaResponse> colas, CategoriaCola categoria, String nombre) {
        consultar(nombre).ifPresent(info -> colas.add(
                new ColaResponse(nombre, categoria, info.getMessageCount(), info.getConsumerCount())));
    }

    private Optional<QueueInformation> consultar(String nombre) {
        try {
            QueueInformation info = rabbitAdmin.getQueueInfo(nombre);
            if (info == null) {
                log.warn("La cola {} no existe en el broker y se omite del listado", nombre);
            }
            return Optional.ofNullable(info);
        } catch (AmqpException ex) {
            log.warn("No se pudo consultar la cola {}: {}", nombre, ex.getClass().getName());
            return Optional.empty();
        }
    }
}
