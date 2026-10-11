package com.pedidos360.rabbitadmin.service;

import com.pedidos360.rabbitadmin.config.RecursosProtegidosProperties;
import com.pedidos360.rabbitadmin.dto.BindingRequest;
import com.pedidos360.rabbitadmin.dto.ColaInfoResponse;
import com.pedidos360.rabbitadmin.dto.ColaResponse;
import com.pedidos360.rabbitadmin.dto.CrearColaRequest;
import com.pedidos360.rabbitadmin.dto.CrearExchangeRequest;
import com.pedidos360.rabbitadmin.dto.ExchangeResponse;
import com.pedidos360.rabbitadmin.dto.PatronesRabbit;
import com.pedidos360.rabbitadmin.exception.RecursoNoEncontradoException;
import com.pedidos360.rabbitadmin.exception.RecursoProtegidoException;
import com.pedidos360.rabbitadmin.exception.SolicitudInvalidaException;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

@Service
public class RabbitAdminService {

    private static final String TIPO_DESTINO_COLA = "queue";

    private final RabbitAdmin rabbitAdmin;
    private final RecursosProtegidosProperties recursosProtegidos;

    public RabbitAdminService(RabbitAdmin rabbitAdmin, RecursosProtegidosProperties recursosProtegidos) {
        this.rabbitAdmin = rabbitAdmin;
        this.recursosProtegidos = recursosProtegidos;
    }

    public ColaResponse crearCola(CrearColaRequest request) {
        validarNombre("nombre", request.nombre());
        boolean conDeadLetter = hayTexto(request.deadLetterExchange());
        if (conDeadLetter) {
            validarNombre("deadLetterExchange", request.deadLetterExchange());
        } else if (hayTexto(request.deadLetterRoutingKey())) {
            throw new SolicitudInvalidaException("deadLetterRoutingKey requiere deadLetterExchange");
        }

        boolean durable = esDurable(request.durable());
        QueueBuilder builder = durable ? QueueBuilder.durable(request.nombre()) : QueueBuilder.nonDurable(request.nombre());
        if (conDeadLetter) {
            builder.deadLetterExchange(request.deadLetterExchange());
        }
        if (hayTexto(request.deadLetterRoutingKey())) {
            builder.deadLetterRoutingKey(request.deadLetterRoutingKey());
        }
        rabbitAdmin.declareQueue(builder.build());

        return new ColaResponse(request.nombre(), durable,
                conDeadLetter ? request.deadLetterExchange() : null,
                hayTexto(request.deadLetterRoutingKey()) ? request.deadLetterRoutingKey() : null);
    }

    public ColaInfoResponse obtenerCola(String nombre) {
        validarNombre("nombre", nombre);
        QueueInformation info = rabbitAdmin.getQueueInfo(nombre);
        if (info == null) {
            throw new RecursoNoEncontradoException("Cola no encontrada: " + nombre);
        }
        return new ColaInfoResponse(info.getName(), info.getMessageCount(), info.getConsumerCount());
    }

    public void eliminarCola(String nombre) {
        validarNombre("nombre", nombre);
        exigirNoProtegido(nombre);
        obtenerCola(nombre);
        rabbitAdmin.deleteQueue(nombre);
    }

    public ExchangeResponse crearExchange(CrearExchangeRequest request) {
        validarNombre("nombre", request.nombre());
        boolean durable = esDurable(request.durable());
        rabbitAdmin.declareExchange(construirExchange(request.tipo(), request.nombre(), durable));
        return new ExchangeResponse(request.nombre(), request.tipo(), durable);
    }

    public void eliminarExchange(String nombre) {
        validarNombre("nombre", nombre);
        exigirNoProtegido(nombre);
        exigirExchangeExistente(nombre);
        rabbitAdmin.deleteExchange(nombre);
    }

    public BindingRequest crearBinding(BindingRequest request) {
        Binding binding = construirBinding(request);
        rabbitAdmin.declareBinding(binding);
        return describir(binding);
    }

    public void eliminarBinding(BindingRequest request) {
        Binding binding = construirBinding(request);
        exigirNoProtegido(binding.getExchange());
        exigirNoProtegido(binding.getDestination());
        exigirExchangeExistente(binding.getExchange());
        if (binding.isDestinationQueue()) {
            obtenerCola(binding.getDestination());
        } else {
            exigirExchangeExistente(binding.getDestination());
        }
        rabbitAdmin.removeBinding(binding);
    }

    private Exchange construirExchange(String tipo, String nombre, boolean durable) {
        if (!hayTexto(tipo)) {
            throw new SolicitudInvalidaException("tipo es obligatorio");
        }
        ExchangeBuilder builder = switch (tipo) {
            case ExchangeTypes.DIRECT -> ExchangeBuilder.directExchange(nombre);
            case ExchangeTypes.TOPIC -> ExchangeBuilder.topicExchange(nombre);
            case ExchangeTypes.FANOUT -> ExchangeBuilder.fanoutExchange(nombre);
            case ExchangeTypes.HEADERS -> ExchangeBuilder.headersExchange(nombre);
            default -> throw new SolicitudInvalidaException("tipo debe ser direct, topic, fanout o headers");
        };
        return builder.durable(durable).build();
    }

    private Binding construirBinding(BindingRequest request) {
        validarNombre("exchange", request.exchange());
        validarNombre("destino", request.destino());
        String tipoDestino = hayTexto(request.tipoDestino()) ? request.tipoDestino() : TIPO_DESTINO_COLA;
        if (!tipoDestino.matches(PatronesRabbit.TIPO_DESTINO)) {
            throw new SolicitudInvalidaException("tipoDestino debe ser queue o exchange");
        }
        DestinationType destinationType = TIPO_DESTINO_COLA.equals(tipoDestino)
                ? DestinationType.QUEUE
                : DestinationType.EXCHANGE;
        String routingKey = request.routingKey() == null ? "" : request.routingKey();
        return new Binding(request.destino(), destinationType, request.exchange(), routingKey, null);
    }

    private BindingRequest describir(Binding binding) {
        return new BindingRequest(binding.getExchange(), binding.getDestination(),
                binding.getDestinationType().name().toLowerCase(), binding.getRoutingKey());
    }

    private void validarNombre(String campo, String valor) {
        if (!hayTexto(valor)) {
            throw new SolicitudInvalidaException(campo + " es obligatorio");
        }
        if (!valor.matches(PatronesRabbit.NOMBRE)) {
            throw new SolicitudInvalidaException(
                    campo + " solo admite letras, numeros, punto, guion y guion bajo (maximo 255)");
        }
    }

    private void exigirNoProtegido(String nombre) {
        if (recursosProtegidos.esProtegido(nombre)) {
            throw new RecursoProtegidoException("El recurso " + nombre + " esta protegido y no se puede eliminar");
        }
    }

    private void exigirExchangeExistente(String nombre) {
        if (!existeExchange(nombre)) {
            throw new RecursoNoEncontradoException("Exchange no encontrado: " + nombre);
        }
    }

    private boolean existeExchange(String nombre) {
        try {
            return Boolean.TRUE.equals(rabbitAdmin.getRabbitTemplate()
                    .execute(channel -> channel.exchangeDeclarePassive(nombre) != null));
        } catch (AmqpIOException ex) {
            return false;
        }
    }

    private boolean hayTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private boolean esDurable(Boolean durable) {
        return durable == null || durable;
    }
}
