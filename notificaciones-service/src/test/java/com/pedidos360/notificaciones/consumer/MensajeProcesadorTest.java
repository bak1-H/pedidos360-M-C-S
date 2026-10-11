package com.pedidos360.notificaciones.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pedidos360.notificaciones.event.PedidoCreadoEvent;
import com.rabbitmq.client.Channel;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class MensajeProcesadorTest {

    private static final long TAG = 7L;

    @Mock
    private Channel channel;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final MensajeProcesador procesador = new MensajeProcesador(
            objectMapper, Validation.buildDefaultValidatorFactory().getValidator());

    private Message mensaje(String cuerpo, boolean redelivered) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setRedelivered(redelivered);
        return new Message(cuerpo.getBytes(StandardCharsets.UTF_8), propiedades);
    }

    private String pedidoValido() {
        return "{\"pedidoId\":\"" + UUID.randomUUID() + "\",\"clienteId\":\"" + UUID.randomUUID()
                + "\",\"direccionDestino\":\"Calle Falsa 456\"}";
    }

    @Test
    void confirmaCuandoElMensajeSeProcesaBien() throws IOException {
        AtomicReference<PedidoCreadoEvent> recibido = new AtomicReference<>();

        procesador.procesar(mensaje(pedidoValido(), false), channel, TAG, PedidoCreadoEvent.class, recibido::set);

        assertThat(recibido.get().direccionDestino()).isEqualTo("Calle Falsa 456");
        verify(channel).basicAck(TAG, false);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void descartaSinReencolarCuandoElPayloadEsInvalido() throws IOException {
        procesador.procesar(mensaje("{\"pedidoId\":null}", false), channel, TAG, PedidoCreadoEvent.class, e -> { });
        procesador.procesar(mensaje("no es json", false), channel, TAG, PedidoCreadoEvent.class, e -> { });

        verify(channel, times(2)).basicNack(TAG, false, false);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void reencolaLaPrimeraFallaTransitoria() throws IOException {
        procesador.procesar(mensaje(pedidoValido(), false), channel, TAG, PedidoCreadoEvent.class, e -> {
            throw new IllegalStateException("servicio caido");
        });

        verify(channel).basicNack(TAG, false, true);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void descartaLaFallaTransitoriaCuandoYaFueReentregado() throws IOException {
        procesador.procesar(mensaje(pedidoValido(), true), channel, TAG, PedidoCreadoEvent.class, e -> {
            throw new IllegalStateException("servicio caido");
        });

        verify(channel).basicNack(TAG, false, false);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void descartaSinReencolarCuandoLaFallaEsUnError() throws IOException {
        procesador.procesar(mensaje(pedidoValido(), false), channel, TAG, PedidoCreadoEvent.class, e -> {
            throw new AssertionError("falla fatal");
        });

        verify(channel).basicNack(TAG, false, false);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void laPoliticaDecideSegunLaFallaYElReintento() {
        assertThat(PoliticaAck.decidir(null, false)).isEqualTo(AccionAck.CONFIRMAR);
        assertThat(PoliticaAck.decidir(new PayloadInvalidoException("x"), false)).isEqualTo(AccionAck.DESCARTAR);
        assertThat(PoliticaAck.decidir(new RuntimeException(), false)).isEqualTo(AccionAck.REENCOLAR);
        assertThat(PoliticaAck.decidir(new RuntimeException(), true)).isEqualTo(AccionAck.DESCARTAR);
    }
}
