package com.pedidos360.bff.client;

import com.pedidos360.bff.dto.CrearColaRequest;
import com.pedidos360.bff.exception.RabbitAdminRechazoException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RabbitAdminClientTest {

    private RabbitAdminClient clienteQueResponde(HttpStatus status, String cuerpo) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(solicitud -> Mono.just(ClientResponse.create(status)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(cuerpo)
                        .build()))
                .build();
        return new RabbitAdminClient(webClient);
    }

    private final CrearColaRequest solicitud = new CrearColaRequest("cola.prueba", null, null, null);

    @Test
    void propagaElEstadoYElMensajeDeUnaRespuesta400() {
        RabbitAdminClient client = clienteQueResponde(HttpStatus.BAD_REQUEST,
                "{\"error\":\"bad_request\",\"message\":\"nombre es obligatorio\"}");

        assertThatThrownBy(() -> client.crearCola(solicitud))
                .isInstanceOfSatisfying(RabbitAdminRechazoException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(400);
                    assertThat(ex.getMessage()).isEqualTo("nombre es obligatorio");
                });
    }

    @Test
    void propagaElEstadoYElMensajeDeUnaRespuesta409() {
        RabbitAdminClient client = clienteQueResponde(HttpStatus.CONFLICT,
                "{\"error\":\"protected_resource\",\"message\":\"El recurso amq.topic esta protegido\"}");

        assertThatThrownBy(() -> client.eliminarExchange("amq.topic"))
                .isInstanceOfSatisfying(RabbitAdminRechazoException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(409);
                    assertThat(ex.getMessage()).contains("protegido");
                });
    }

    @Test
    void unaRespuesta401DelServicioNoSeReenviaAlUsuario() {
        RabbitAdminClient client = clienteQueResponde(HttpStatus.UNAUTHORIZED, "{\"message\":\"token invalido\"}");

        assertThatThrownBy(() -> client.crearCola(solicitud))
                .isInstanceOf(WebClientResponseException.class);
    }

    @Test
    void unaRespuesta403DelServicioNoSeReenviaAlUsuario() {
        RabbitAdminClient client = clienteQueResponde(HttpStatus.FORBIDDEN, "{\"message\":\"prohibido\"}");

        assertThatThrownBy(() -> client.crearCola(solicitud))
                .isInstanceOf(WebClientResponseException.class);
    }

    @Test
    void unaFallaDelServidorSigueSiendoWebClientResponseException() {
        RabbitAdminClient client = clienteQueResponde(HttpStatus.BAD_GATEWAY, "{\"message\":\"broker\"}");

        assertThatThrownBy(() -> client.crearCola(solicitud))
                .isInstanceOf(WebClientResponseException.class);
    }
}
