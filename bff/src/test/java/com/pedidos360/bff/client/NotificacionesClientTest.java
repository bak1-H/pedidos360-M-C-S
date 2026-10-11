package com.pedidos360.bff.client;

import com.pedidos360.bff.exception.NotificacionesRechazoException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificacionesClientTest {

    private NotificacionesClient clienteQueResponde(HttpStatus status, String cuerpo,
                                                    AtomicReference<ClientRequest> capturada) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(solicitud -> {
                    capturada.set(solicitud);
                    return Mono.just(ClientResponse.create(status)
                            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .body(cuerpo)
                            .build());
                })
                .build();
        return new NotificacionesClient(webClient);
    }

    @Test
    void pasaLosParametrosDeConsultaAlServicio() {
        AtomicReference<ClientRequest> capturada = new AtomicReference<>();
        NotificacionesClient client = clienteQueResponde(HttpStatus.OK, "{\"pagina\":1}", capturada);

        client.listar("PEDIDO_CREADO", null, "1", "5");

        assertThat(capturada.get().url().getPath()).isEqualTo("/internal/notificaciones");
        assertThat(capturada.get().url().getQuery()).isEqualTo("tipo=PEDIDO_CREADO&pagina=1&tamano=5");
    }

    @Test
    void propagaElEstadoYElMensajeDeUn400() {
        NotificacionesClient client = clienteQueResponde(HttpStatus.BAD_REQUEST,
                "{\"message\":\"tamaño debe estar entre 1 y 100\"}", new AtomicReference<>());

        assertThatThrownBy(() -> client.listar(null, null, null, "500"))
                .isInstanceOfSatisfying(NotificacionesRechazoException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(400);
                    assertThat(ex.getMessage()).isEqualTo("tamaño debe estar entre 1 y 100");
                });
    }

    @Test
    void noReenviaUn401NiUn403DelServicioInterno() {
        NotificacionesClient noAutorizado = clienteQueResponde(HttpStatus.UNAUTHORIZED, "{}", new AtomicReference<>());
        NotificacionesClient prohibido = clienteQueResponde(HttpStatus.FORBIDDEN, "{}", new AtomicReference<>());

        assertThatThrownBy(noAutorizado::resumir).isInstanceOf(WebClientResponseException.class);
        assertThatThrownBy(prohibido::listarColas).isInstanceOf(WebClientResponseException.class);
    }

    @Test
    void unaFallaDelServidorSigueSiendoWebClientResponseException() {
        NotificacionesClient client = clienteQueResponde(HttpStatus.BAD_GATEWAY, "{}", new AtomicReference<>());

        assertThatThrownBy(client::resumir).isInstanceOf(WebClientResponseException.class);
    }
}
