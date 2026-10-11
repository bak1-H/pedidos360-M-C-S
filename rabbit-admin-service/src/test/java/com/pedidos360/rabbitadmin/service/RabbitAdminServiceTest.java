package com.pedidos360.rabbitadmin.service;

import com.pedidos360.rabbitadmin.config.RecursosProtegidosProperties;
import com.pedidos360.rabbitadmin.dto.BindingRequest;
import com.pedidos360.rabbitadmin.dto.ColaInfoResponse;
import com.pedidos360.rabbitadmin.dto.CrearColaRequest;
import com.pedidos360.rabbitadmin.dto.CrearExchangeRequest;
import com.pedidos360.rabbitadmin.exception.RecursoNoEncontradoException;
import com.pedidos360.rabbitadmin.exception.RecursoProtegidoException;
import com.pedidos360.rabbitadmin.exception.SolicitudInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpIOException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitAdminServiceTest {

    @Mock
    private RabbitAdmin rabbitAdmin;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private final RecursosProtegidosProperties protegidos = new RecursosProtegidosProperties(
            List.of("pedidos360.eventos", "pedidos360.eventos.dlx"), List.of("amq.", "notificaciones.", "tickets."));

    private void exchangeExiste(boolean existe) {
        when(rabbitAdmin.getRabbitTemplate()).thenReturn(rabbitTemplate);
        if (existe) {
            when(rabbitTemplate.execute(ArgumentMatchers.<ChannelCallback<Boolean>>any())).thenReturn(true);
        } else {
            when(rabbitTemplate.execute(ArgumentMatchers.<ChannelCallback<Boolean>>any()))
                    .thenThrow(new AmqpIOException(new IOException("NOT_FOUND")));
        }
    }

    @Test
    void creaColaDurableConDeadLetter() {
        RabbitAdminService service = new RabbitAdminService(rabbitAdmin, protegidos);
        ArgumentCaptor<Queue> captor = ArgumentCaptor.forClass(Queue.class);

        service.crearCola(new CrearColaRequest("cola.prueba", null, "dlx.prueba", "cola.prueba.dlq"));

        verify(rabbitAdmin).declareQueue(captor.capture());
        Queue cola = captor.getValue();
        assertThat(cola.getName()).isEqualTo("cola.prueba");
        assertThat(cola.isDurable()).isTrue();
        assertThat(cola.getArguments())
                .containsEntry("x-dead-letter-exchange", "dlx.prueba")
                .containsEntry("x-dead-letter-routing-key", "cola.prueba.dlq");
    }

    @Test
    void rechazaColaConNombreEnBlancoOInvalido() {
        RabbitAdminService service = new RabbitAdminService(rabbitAdmin, protegidos);

        assertThatThrownBy(() -> service.crearCola(new CrearColaRequest("  ", null, null, null)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("nombre es obligatorio");
        assertThatThrownBy(() -> service.crearCola(new CrearColaRequest("con espacios", null, null, null)))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> service.crearCola(new CrearColaRequest("cola", null, null, "sin.dlx")))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("deadLetterExchange");

        verify(rabbitAdmin, never()).declareQueue(any());
    }

    @Test
    void eliminaColaExistenteYFallaSiNoExiste() {
        RabbitAdminService service = new RabbitAdminService(rabbitAdmin, protegidos);
        when(rabbitAdmin.getQueueInfo("existe")).thenReturn(new QueueInformation("existe", 3, 1));
        when(rabbitAdmin.getQueueInfo("fantasma")).thenReturn(null);

        service.eliminarCola("existe");

        verify(rabbitAdmin).deleteQueue("existe");
        assertThatThrownBy(() -> service.eliminarCola("fantasma"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(rabbitAdmin, never()).deleteQueue("fantasma");
    }

    @Test
    void consultaLaCantidadDeMensajesYConsumidores() {
        RabbitAdminService service = new RabbitAdminService(rabbitAdmin, protegidos);
        when(rabbitAdmin.getQueueInfo("existe")).thenReturn(new QueueInformation("existe", 3, 1));

        ColaInfoResponse info = service.obtenerCola("existe");

        assertThat(info.mensajes()).isEqualTo(3);
        assertThat(info.consumidores()).isEqualTo(1);
    }

    @Test
    void creaExchangeDelTipoPedido() {
        RabbitAdminService service = new RabbitAdminService(rabbitAdmin, protegidos);
        ArgumentCaptor<Exchange> captor = ArgumentCaptor.forClass(Exchange.class);

        service.crearExchange(new CrearExchangeRequest("eventos.prueba", "topic", null));

        verify(rabbitAdmin).declareExchange(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo("topic");
        assertThat(captor.getValue().isDurable()).isTrue();
    }

    @Test
    void rechazaExchangeConTipoInvalidoONombreEnBlanco() {
        RabbitAdminService service = new RabbitAdminService(rabbitAdmin, protegidos);

        assertThatThrownBy(() -> service.crearExchange(new CrearExchangeRequest("ex", "random", null)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("tipo");
        assertThatThrownBy(() -> service.crearExchange(new CrearExchangeRequest("", "topic", null)))
                .isInstanceOf(SolicitudInvalidaException.class);

        verify(rabbitAdmin, never()).declareExchange(any());
    }

    @Test
    void eliminaExchange() {
        RabbitAdminService service = new RabbitAdminService(rabbitAdmin, protegidos);
        exchangeExiste(true);

        service.eliminarExchange("eventos.prueba");

        verify(rabbitAdmin).deleteExchange("eventos.prueba");
    }

    @Test
    void creaYEliminaBindingHaciaUnaCola() {
        RabbitAdminService service = new RabbitAdminService(rabbitAdmin, protegidos);
        BindingRequest request = new BindingRequest("eventos.prueba", "cola.prueba", null, "pedido.creado");
        ArgumentCaptor<Binding> captor = ArgumentCaptor.forClass(Binding.class);
        exchangeExiste(true);
        when(rabbitAdmin.getQueueInfo("cola.prueba")).thenReturn(new QueueInformation("cola.prueba", 0, 0));

        service.crearBinding(request);
        service.eliminarBinding(request);

        verify(rabbitAdmin).declareBinding(captor.capture());
        verify(rabbitAdmin).removeBinding(any(Binding.class));
        Binding binding = captor.getValue();
        assertThat(binding.getExchange()).isEqualTo("eventos.prueba");
        assertThat(binding.getDestination()).isEqualTo("cola.prueba");
        assertThat(binding.getDestinationType()).isEqualTo(Binding.DestinationType.QUEUE);
        assertThat(binding.getRoutingKey()).isEqualTo("pedido.creado");
    }

    @Test
    void rechazaBindingSinDestinoOExchange() {
        RabbitAdminService service = new RabbitAdminService(rabbitAdmin, protegidos);

        assertThatThrownBy(() -> service.crearBinding(new BindingRequest("ex", " ", null, "k")))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("destino");
        assertThatThrownBy(() -> service.crearBinding(new BindingRequest(null, "cola", null, "k")))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("exchange");

        verify(rabbitAdmin, never()).declareBinding(any(Binding.class));
    }

    @Test
    void noEliminaRecursosProtegidos() {
        RabbitAdminService service = new RabbitAdminService(rabbitAdmin, protegidos);

        assertThatThrownBy(() -> service.eliminarExchange("pedidos360.eventos"))
                .isInstanceOf(RecursoProtegidoException.class);
        assertThatThrownBy(() -> service.eliminarExchange("pedidos360.eventos.dlx"))
                .isInstanceOf(RecursoProtegidoException.class);
        assertThatThrownBy(() -> service.eliminarExchange("amq.topic"))
                .isInstanceOf(RecursoProtegidoException.class);
        assertThatThrownBy(() -> service.eliminarCola("amq.gen-abc"))
                .isInstanceOf(RecursoProtegidoException.class);
        assertThatThrownBy(() -> service.eliminarCola("notificaciones.pedido.creado"))
                .isInstanceOf(RecursoProtegidoException.class);
        assertThatThrownBy(() -> service.eliminarCola("notificaciones.pedido.creado.dlq"))
                .isInstanceOf(RecursoProtegidoException.class);
        assertThatThrownBy(() -> service.eliminarCola("notificaciones.pedido.creado.dlq.parked"))
                .isInstanceOf(RecursoProtegidoException.class);
        assertThatThrownBy(() -> service.eliminarCola("tickets.envio.entregado"))
                .isInstanceOf(RecursoProtegidoException.class);

        verify(rabbitAdmin, never()).deleteExchange(any());
        verify(rabbitAdmin, never()).deleteQueue(any());
    }

    @Test
    void noEliminaBindingsSobreUnExchangeProtegido() {
        RabbitAdminService service = new RabbitAdminService(rabbitAdmin, protegidos);

        assertThatThrownBy(() -> service.eliminarBinding(
                new BindingRequest("pedidos360.eventos", "cola.prueba", null, "pedido.creado")))
                .isInstanceOf(RecursoProtegidoException.class);

        verify(rabbitAdmin, never()).removeBinding(any(Binding.class));
    }

    @Test
    void noEliminaBindingsHaciaUnDestinoProtegido() {
        RabbitAdminService service = new RabbitAdminService(rabbitAdmin, protegidos);

        assertThatThrownBy(() -> service.eliminarBinding(
                new BindingRequest("eventos.prueba", "notificaciones.pedido.creado", null, "pedido.creado")))
                .isInstanceOf(RecursoProtegidoException.class);

        verify(rabbitAdmin, never()).removeBinding(any(Binding.class));
    }

    @Test
    void unaColaPersonalizadaSePuedeCrearYEliminar() {
        RabbitAdminService service = new RabbitAdminService(rabbitAdmin, protegidos);
        when(rabbitAdmin.getQueueInfo("mi.cola.personal")).thenReturn(new QueueInformation("mi.cola.personal", 0, 0));

        service.crearCola(new CrearColaRequest("mi.cola.personal", null, null, null));
        service.eliminarCola("mi.cola.personal");

        verify(rabbitAdmin).declareQueue(any(Queue.class));
        verify(rabbitAdmin).deleteQueue("mi.cola.personal");
    }

    @Test
    void eliminarExchangeInexistenteDevuelveNoEncontrado() {
        RabbitAdminService service = new RabbitAdminService(rabbitAdmin, protegidos);
        exchangeExiste(false);

        assertThatThrownBy(() -> service.eliminarExchange("fantasma"))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(rabbitAdmin, never()).deleteExchange("fantasma");
    }

    @Test
    void eliminarBindingConDestinoInexistenteDevuelveNoEncontrado() {
        RabbitAdminService service = new RabbitAdminService(rabbitAdmin, protegidos);
        exchangeExiste(true);
        when(rabbitAdmin.getQueueInfo("fantasma")).thenReturn(null);

        assertThatThrownBy(() -> service.eliminarBinding(new BindingRequest("eventos.prueba", "fantasma", null, "k")))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(rabbitAdmin, never()).removeBinding(any(Binding.class));
    }

    @Test
    void eliminarBindingConExchangeInexistenteDevuelveNoEncontrado() {
        RabbitAdminService service = new RabbitAdminService(rabbitAdmin, protegidos);
        exchangeExiste(false);

        assertThatThrownBy(() -> service.eliminarBinding(new BindingRequest("fantasma", "cola", null, "k")))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verify(rabbitAdmin, never()).removeBinding(any(Binding.class));
    }
}
