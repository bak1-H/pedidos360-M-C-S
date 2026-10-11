package com.pedidos360.notificaciones.consumer.notificacion;

import com.pedidos360.notificaciones.config.RabbitTopologyProperties;
import com.pedidos360.notificaciones.consumer.MensajeProcesador;
import com.pedidos360.notificaciones.event.PedidoCreadoEvent;
import com.pedidos360.notificaciones.model.TipoNotificacion;
import com.pedidos360.notificaciones.service.NotificacionService;
import com.pedidos360.notificaciones.service.Sanitizador;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class NotificacionConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificacionConsumer.class);

    private static final int LONGITUD_TEXTO_LIBRE = 200;

    private final MensajeProcesador procesador;
    private final NotificacionService notificacionService;
    private final RabbitTopologyProperties topology;

    public NotificacionConsumer(MensajeProcesador procesador, NotificacionService notificacionService,
                                RabbitTopologyProperties topology) {
        this.procesador = procesador;
        this.notificacionService = notificacionService;
        this.topology = topology;
    }

    @RabbitListener(queues = "${rabbitmq.pedido-creado.queue}")
    public void alCrearPedido(Message message, Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        procesador.procesar(message, channel, deliveryTag, PedidoCreadoEvent.class, this::notificarPedidoCreado);
    }

    private void notificarPedidoCreado(PedidoCreadoEvent event) {
        String destino = texto(event.direccionDestino());
        log.info("NOTIFICACION al cliente {}: tu pedido {} fue creado con destino {}",
                event.clienteId(), event.pedidoId(), destino);
        notificacionService.registrarProcesada(TipoNotificacion.PEDIDO_CREADO, topology.pedidoCreado(),
                String.valueOf(event.clienteId()),
                "Tu pedido " + event.pedidoId() + " fue creado con destino " + destino,
                event.pedidoId());
    }

    private String texto(String valor) {
        return Sanitizador.limpiar(valor, LONGITUD_TEXTO_LIBRE);
    }
}
