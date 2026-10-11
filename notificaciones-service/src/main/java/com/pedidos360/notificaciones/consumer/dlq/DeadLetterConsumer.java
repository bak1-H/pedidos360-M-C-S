package com.pedidos360.notificaciones.consumer.dlq;

import com.pedidos360.notificaciones.config.RabbitTopologyProperties;
import com.pedidos360.notificaciones.model.TipoNotificacion;
import com.pedidos360.notificaciones.service.NotificacionService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    private final RabbitTopologyProperties topology;
    private final NotificacionService notificacionService;

    public DeadLetterConsumer(RabbitTopologyProperties topology, NotificacionService notificacionService) {
        this.topology = topology;
        this.notificacionService = notificacionService;
    }

    @RabbitListener(queues = "${rabbitmq.pedido-creado.dead-letter-queue}")
    public void pedidoCreadoMuerto(Message message, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        MessageProperties propiedades = message.getMessageProperties();
        log.error("Mensaje muerto en {} (messageId {}, routing key original {}, x-death {})",
                propiedades.getConsumerQueue(),
                propiedades.getMessageId(),
                propiedades.getReceivedRoutingKey(),
                resumirXDeath(propiedades.getXDeathHeader()));
        notificacionService.registrarMuerta(TipoNotificacion.PEDIDO_CREADO, topology.pedidoCreado(),
                motivoDeMuerte(propiedades.getXDeathHeader()));
        channel.basicAck(deliveryTag, false);
    }

    private String motivoDeMuerte(List<Map<String, ?>> xDeath) {
        if (xDeath == null || xDeath.isEmpty() || xDeath.get(0).get("reason") == null) {
            return "desconocido";
        }
        return String.valueOf(xDeath.get(0).get("reason"));
    }

    private String resumirXDeath(List<Map<String, ?>> xDeath) {
        if (xDeath == null) {
            return "[]";
        }
        return xDeath.stream()
                .map(entrada -> entrada.get("queue") + "/" + entrada.get("reason") + "/" + entrada.get("count"))
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
