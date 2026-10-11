package com.pedidos360.notificaciones.consumer.notificacion;

import com.pedidos360.notificaciones.consumer.MensajeProcesador;
import com.pedidos360.notificaciones.event.EnvioAsignadoEvent;
import com.pedidos360.notificaciones.event.PedidoCreadoEvent;
import com.pedidos360.notificaciones.event.UsuarioCreadoEvent;
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

    private final MensajeProcesador procesador;

    public NotificacionConsumer(MensajeProcesador procesador) {
        this.procesador = procesador;
    }

    @RabbitListener(queues = "${rabbitmq.pedido-creado.queue}")
    public void alCrearPedido(Message message, Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        procesador.procesar(message, channel, deliveryTag, PedidoCreadoEvent.class, this::notificarPedidoCreado);
    }

    @RabbitListener(queues = "${rabbitmq.envio-asignado.queue}")
    public void alAsignarEnvio(Message message, Channel channel,
                               @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        procesador.procesar(message, channel, deliveryTag, EnvioAsignadoEvent.class, this::notificarEnvioAsignado);
    }

    @RabbitListener(queues = "${rabbitmq.usuario-creado.queue}")
    public void alCrearUsuario(Message message, Channel channel,
                               @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        procesador.procesar(message, channel, deliveryTag, UsuarioCreadoEvent.class, this::notificarUsuarioCreado);
    }

    private void notificarPedidoCreado(PedidoCreadoEvent event) {
        log.info("NOTIFICACION al cliente {}: tu pedido {} fue creado con destino {}",
                event.clienteId(), event.pedidoId(), event.direccionDestino());
    }

    private void notificarEnvioAsignado(EnvioAsignadoEvent event) {
        log.info("NOTIFICACION al repartidor {}: se te asigno el envio {} del pedido {}",
                event.repartidorId(), event.envioId(), event.pedidoId());
    }

    private void notificarUsuarioCreado(UsuarioCreadoEvent event) {
        log.info("NOTIFICACION a {}: te damos la bienvenida a Pedidos360, {}", event.email(), event.nombre());
    }
}
