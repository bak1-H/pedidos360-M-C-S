package com.pedidos360.pedidos.consumer.envio;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.pedidos.consumer.MensajeProcesador;
import com.pedidos360.pedidos.service.PedidoService;
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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class EnvioEventConsumerTest {

    private static final long TAG = 4L;

    @Mock
    private Channel channel;

    @Mock
    private PedidoService pedidoService;

    private EnvioEventConsumer consumer() {
        return new EnvioEventConsumer(
                new MensajeProcesador(
                        new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false),
                        Validation.buildDefaultValidatorFactory().getValidator()),
                pedidoService);
    }

    private Message mensaje(String cuerpo, boolean redelivered) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setRedelivered(redelivered);
        return new Message(cuerpo.getBytes(StandardCharsets.UTF_8), propiedades);
    }

    @Test
    void envioAsignadoValidoSincronizaYConfirma() throws IOException {
        UUID pedidoId = UUID.randomUUID();
        String cuerpo = "{\"envioId\":\"" + UUID.randomUUID() + "\",\"pedidoId\":\"" + pedidoId
                + "\",\"repartidorId\":\"" + UUID.randomUUID() + "\",\"fechaEntregaEstimada\":null}";

        consumer().alAsignarEnvio(mensaje(cuerpo, false), channel, TAG);

        verify(pedidoService).sincronizarConEnvioAsignado(pedidoId);
        verify(channel).basicAck(TAG, false);
        verifyNoMoreInteractions(channel);
    }

    @Test
    void cambioDeEstadoValidoSincronizaYConfirma() throws IOException {
        UUID pedidoId = UUID.randomUUID();
        String cuerpo = "{\"envioId\":\"" + UUID.randomUUID() + "\",\"pedidoId\":\"" + pedidoId
                + "\",\"estadoEnvio\":\"ENTREGADO\",\"descripcion\":\"ok\",\"fecha\":\"2026-10-10T20:00:00\"}";

        consumer().alCambiarEstadoDeEnvio(mensaje(cuerpo, false), channel, TAG);

        verify(pedidoService).sincronizarConEstadoDeEnvio(pedidoId, "ENTREGADO");
        verify(channel).basicAck(TAG, false);
    }

    @Test
    void unPayloadInvalidoSeDescartaHaciaLaDlqSinTocarElPedido() throws IOException {
        consumer().alAsignarEnvio(mensaje("{\"pedidoId\":null}", false), channel, TAG);
        consumer().alCambiarEstadoDeEnvio(mensaje("no es json", false), channel, TAG);
        consumer().alCambiarEstadoDeEnvio(mensaje("{\"pedidoId\":\"" + UUID.randomUUID() + "\"}", false), channel, TAG);

        verify(channel, org.mockito.Mockito.times(3)).basicNack(TAG, false, false);
        verifyNoMoreInteractions(channel);
        verifyNoInteractions(pedidoService);
    }

    @Test
    void unaFallaTransitoriaSeReencolaUnaVezYLuegoSeDescarta() throws IOException {
        UUID pedidoId = UUID.randomUUID();
        doThrow(new IllegalStateException("base caida")).when(pedidoService).sincronizarConEnvioAsignado(pedidoId);
        String cuerpo = "{\"envioId\":null,\"pedidoId\":\"" + pedidoId + "\"}";

        consumer().alAsignarEnvio(mensaje(cuerpo, false), channel, TAG);
        verify(channel).basicNack(TAG, false, true);

        consumer().alAsignarEnvio(mensaje(cuerpo, true), channel, TAG);
        verify(channel).basicNack(TAG, false, false);
    }
}
