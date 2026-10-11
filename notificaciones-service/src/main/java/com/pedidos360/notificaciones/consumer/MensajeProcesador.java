package com.pedidos360.notificaciones.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;

@Component
public class MensajeProcesador {

    private static final Logger log = LoggerFactory.getLogger(MensajeProcesador.class);

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public MensajeProcesador(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public <T> void procesar(Message message, Channel channel, long deliveryTag,
                             Class<T> tipo, Consumer<T> accion) throws IOException {
        boolean redelivered = message.getMessageProperties().isRedelivered();
        Throwable falla = ejecutar(message, tipo, accion);
        AccionAck decision = PoliticaAck.decidir(falla, redelivered);
        registrar(decision, message, falla);
        aplicar(decision, channel, deliveryTag);
    }

    private <T> Throwable ejecutar(Message message, Class<T> tipo, Consumer<T> accion) {
        try {
            accion.accept(leer(message, tipo));
            return null;
        } catch (Throwable ex) {
            return ex;
        }
    }

    private <T> T leer(Message message, Class<T> tipo) {
        T payload;
        try {
            payload = objectMapper.readValue(message.getBody(), tipo);
        } catch (IOException ex) {
            throw new PayloadInvalidoException("El cuerpo no es un JSON valido para " + tipo.getSimpleName(), ex);
        }
        if (payload == null) {
            throw new PayloadInvalidoException("El cuerpo esta vacio");
        }
        Set<ConstraintViolation<T>> violaciones = validator.validate(payload);
        if (!violaciones.isEmpty()) {
            throw new PayloadInvalidoException("Campos invalidos en " + tipo.getSimpleName() + ": " + violaciones);
        }
        return payload;
    }

    private void registrar(AccionAck decision, Message message, Throwable falla) {
        String cola = message.getMessageProperties().getConsumerQueue();
        switch (decision) {
            case CONFIRMAR -> log.debug("Mensaje de {} confirmado", cola);
            case REENCOLAR -> log.warn("Falla transitoria en {}, se reencola una vez: {}", cola, falla.getMessage(), falla);
            case DESCARTAR -> log.error("Mensaje de {} enviado a la DLQ: {}", cola, falla.getMessage(), falla);
        }
    }

    private void aplicar(AccionAck decision, Channel channel, long deliveryTag) throws IOException {
        switch (decision) {
            case CONFIRMAR -> channel.basicAck(deliveryTag, false);
            case REENCOLAR -> channel.basicNack(deliveryTag, false, true);
            case DESCARTAR -> channel.basicNack(deliveryTag, false, false);
        }
    }
}
