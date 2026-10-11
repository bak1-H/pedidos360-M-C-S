package com.pedidos360.notificaciones.service;

import com.pedidos360.notificaciones.config.RabbitTopologyProperties;
import com.pedidos360.notificaciones.config.RabbitTopologyProperties.Canal;
import com.pedidos360.notificaciones.dto.CategoriaCola;
import com.pedidos360.notificaciones.dto.ColaResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.QueueInformation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ColaServiceTest {

    @Mock
    private AmqpAdmin rabbitAdmin;

    private final RabbitTopologyProperties topology = new RabbitTopologyProperties("ex", "ex.dlx",
            new Canal("q.a", "rk.a", "q.a.dlq", "q.a.dlq"));

    @Test
    void listaLaColaDeNegocioYSuDlqConSuCategoria() {
        ColaService service = new ColaService(rabbitAdmin, topology);
        when(rabbitAdmin.getQueueInfo(anyString())).thenReturn(new QueueInformation("x", 4, 1));

        List<ColaResponse> colas = service.listar();

        assertThat(colas).extracting(ColaResponse::nombre).containsExactly("q.a", "q.a.dlq");
        assertThat(colas).extracting(ColaResponse::categoria)
                .containsExactly(CategoriaCola.NEGOCIO, CategoriaCola.DLQ);
        assertThat(colas.get(0).nombre()).isEqualTo("q.a");
        assertThat(colas.get(0).mensajes()).isEqualTo(4);
    }

    @Test
    void omiteLasColasInexistentesYLasQueFallanSinLanzarExcepcion() {
        ColaService service = new ColaService(rabbitAdmin, topology);
        when(rabbitAdmin.getQueueInfo(anyString())).thenReturn(null);
        when(rabbitAdmin.getQueueInfo("q.a")).thenReturn(new QueueInformation("q.a", 0, 1));
        when(rabbitAdmin.getQueueInfo("q.a.dlq")).thenThrow(new AmqpConnectException(new RuntimeException("caido")));

        List<ColaResponse> colas = service.listar();

        assertThat(colas).extracting(ColaResponse::nombre).containsExactly("q.a");
    }
}
