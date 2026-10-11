package com.pedidos360.notificaciones.consumer.dlq;

import com.pedidos360.notificaciones.config.RabbitTopologyProperties;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Component
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    private static final String DEFAULT_EXCHANGE = "";

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTopologyProperties topology;
    private final Duration confirmTimeout;

    public DeadLetterConsumer(RabbitTemplate rabbitTemplate, RabbitTopologyProperties topology,
                              @Value("${rabbitmq.parked-confirm-timeout}") Duration confirmTimeout) {
        this.rabbitTemplate = rabbitTemplate;
        this.topology = topology;
        this.confirmTimeout = confirmTimeout;
    }

    @RabbitListener(queues = "${rabbitmq.pedido-creado.dead-letter-queue}")
    public void pedidoCreadoMuerto(Message message, Channel channel,
                                   @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        registrarYEstacionar(message, channel, deliveryTag, topology.pedidoCreado().parkedQueue());
    }

    @RabbitListener(queues = "${rabbitmq.envio-asignado.dead-letter-queue}")
    public void envioAsignadoMuerto(Message message, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        registrarYEstacionar(message, channel, deliveryTag, topology.envioAsignado().parkedQueue());
    }

    @RabbitListener(queues = "${rabbitmq.usuario-creado.dead-letter-queue}")
    public void usuarioCreadoMuerto(Message message, Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        registrarYEstacionar(message, channel, deliveryTag, topology.usuarioCreado().parkedQueue());
    }

    @RabbitListener(queues = "${rabbitmq.envio-entregado.dead-letter-queue}")
    public void envioEntregadoMuerto(Message message, Channel channel,
                                     @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        registrarYEstacionar(message, channel, deliveryTag, topology.envioEntregado().parkedQueue());
    }

    private void registrarYEstacionar(Message message, Channel channel, long deliveryTag,
                                      String parkedQueue) throws IOException {
        MessageProperties propiedades = message.getMessageProperties();
        log.error("Mensaje muerto en {} (messageId {}, routing key original {}, x-death {})",
                propiedades.getConsumerQueue(),
                propiedades.getMessageId(),
                propiedades.getReceivedRoutingKey(),
                resumirXDeath(propiedades.getXDeathHeader()));
        if (estacionar(message, parkedQueue)) {
            channel.basicAck(deliveryTag, false);
        } else {
            channel.basicNack(deliveryTag, false, true);
        }
    }

    private boolean estacionar(Message message, String parkedQueue) {
        CorrelationData correlacion = new CorrelationData();
        try {
            rabbitTemplate.send(DEFAULT_EXCHANGE, parkedQueue, message, correlacion);
            CorrelationData.Confirm confirmacion =
                    correlacion.getFuture().get(confirmTimeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!confirmacion.isAck()) {
                log.error("El broker rechazo el mensaje para {}: {}", parkedQueue, confirmacion.getReason());
                return false;
            }
            if (correlacion.getReturned() != null) {
                log.error("El mensaje para {} fue devuelto por el broker: {}", parkedQueue,
                        correlacion.getReturned().getReplyText());
                return false;
            }
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.error("Se interrumpio la espera de confirmacion para {}", parkedQueue);
            return false;
        } catch (TimeoutException | ExecutionException | RuntimeException ex) {
            log.error("No se pudo estacionar el mensaje en {}, se deja en la DLQ: {}", parkedQueue, ex.getMessage(), ex);
            return false;
        }
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
