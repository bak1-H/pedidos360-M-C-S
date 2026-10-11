package com.pedidos360.pedidos.consumer.envio;

import com.pedidos360.pedidos.consumer.MensajeProcesador;
import com.pedidos360.pedidos.event.EnvioAsignadoEvent;
import com.pedidos360.pedidos.event.EnvioEstadoCambiadoEvent;
import com.pedidos360.pedidos.service.PedidoService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class EnvioEventConsumer {

    private final MensajeProcesador procesador;
    private final PedidoService pedidoService;

    public EnvioEventConsumer(MensajeProcesador procesador, PedidoService pedidoService) {
        this.procesador = procesador;
        this.pedidoService = pedidoService;
    }

    @RabbitListener(queues = "${rabbitmq.envio-asignado.queue}")
    public void alAsignarEnvio(Message message, Channel channel,
                               @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        procesador.procesar(message, channel, deliveryTag, EnvioAsignadoEvent.class,
                event -> pedidoService.sincronizarConEnvioAsignado(event.pedidoId()));
    }

    @RabbitListener(queues = "${rabbitmq.envio-estado-cambiado.queue}")
    public void alCambiarEstadoDeEnvio(Message message, Channel channel,
                                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        procesador.procesar(message, channel, deliveryTag, EnvioEstadoCambiadoEvent.class,
                event -> pedidoService.sincronizarConEstadoDeEnvio(event.pedidoId(), event.estadoEnvio()));
    }
}
