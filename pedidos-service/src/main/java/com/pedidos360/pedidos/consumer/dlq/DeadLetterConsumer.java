package com.pedidos360.pedidos.consumer.dlq;

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

    @RabbitListener(queues = "${rabbitmq.envio-asignado.dead-letter-queue}")
    public void envioAsignadoMuerto(Message message, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        registrarYConfirmar(message, channel, deliveryTag);
    }

    @RabbitListener(queues = "${rabbitmq.envio-estado-cambiado.dead-letter-queue}")
    public void envioEstadoCambiadoMuerto(Message message, Channel channel,
                                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        registrarYConfirmar(message, channel, deliveryTag);
    }

    private void registrarYConfirmar(Message message, Channel channel, long deliveryTag) throws IOException {
        MessageProperties propiedades = message.getMessageProperties();
        log.error("Mensaje muerto en {} (messageId {}, routing key original {}, x-death {})",
                propiedades.getConsumerQueue(),
                propiedades.getMessageId(),
                propiedades.getReceivedRoutingKey(),
                resumirXDeath(propiedades.getXDeathHeader()));
        channel.basicAck(deliveryTag, false);
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
