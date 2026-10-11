package com.pedidos360.notificaciones.consumer.ticket;

import com.pedidos360.notificaciones.consumer.MensajeProcesador;
import com.pedidos360.notificaciones.event.EnvioEntregadoEvent;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TicketConsumer {

    private final MensajeProcesador procesador;
    private final TicketService ticketService;

    public TicketConsumer(MensajeProcesador procesador, TicketService ticketService) {
        this.procesador = procesador;
        this.ticketService = ticketService;
    }

    @RabbitListener(queues = "${rabbitmq.envio-entregado.queue}")
    public void alEntregarEnvio(Message message, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        procesador.procesar(message, channel, deliveryTag, EnvioEntregadoEvent.class, ticketService::generar);
    }
}
