package com.pedidos360.bff.client;

import com.pedidos360.bff.dto.CambioEstadoEnvioDto;
import com.pedidos360.bff.dto.EnvioRequestDto;
import com.pedidos360.bff.exception.EnvioRechazoException;
import com.pedidos360.bff.security.IdentidadInterna;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnviosClientTest {

    private final IdentidadInterna identidad = new IdentidadInterna("oid-1", List.of("ADMIN"));

    private EnviosClient clienteQueResponde(HttpStatus status, String cuerpo) {
        WebClient webClient = WebClient.builder()
                .exchangeFunction(solicitud -> Mono.just(ClientResponse.create(status)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body(cuerpo)
                        .build()))
                .build();
        return new EnviosClient(webClient);
    }

    @Test
    void propagaElCodigoYElMensajeDeUn409DeEnvioDuplicado() {
        EnviosClient client = clienteQueResponde(HttpStatus.CONFLICT,
                "{\"error\":\"ENVIO_DUPLICADO\",\"message\":\"Ya existe un envío para el pedido 1234 detalle interno\"}");

        assertThatThrownBy(() -> client.crear(new EnvioRequestDto(UUID.randomUUID(), UUID.randomUUID(), null), identidad))
                .isInstanceOfSatisfying(EnvioRechazoException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(409);
                    assertThat(ex.getCodigo()).isEqualTo("ENVIO_DUPLICADO");
                    assertThat(ex.getMessage()).isEqualTo("Ya existe un envío para el pedido indicado");
                });
    }

    @Test
    void propagaElCodigoDeUn409DeTransicionInvalida() {
        EnviosClient client = clienteQueResponde(HttpStatus.CONFLICT,
                "{\"error\":\"TRANSICION_INVALIDA\",\"message\":\"No se puede cambiar un envío de ENTREGADO a PENDIENTE\"}");

        assertThatThrownBy(() -> client.cambiarEstado(UUID.randomUUID(), new CambioEstadoEnvioDto("PENDIENTE", "x"), identidad))
                .isInstanceOfSatisfying(EnvioRechazoException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(409);
                    assertThat(ex.getCodigo()).isEqualTo("TRANSICION_INVALIDA");
                    assertThat(ex.getMessage()).isEqualTo("El envío no admite ese cambio de estado");
                });
    }

    @Test
    void unaRespuesta404EnBusquedaDevuelveVacioYEnCambioDeEstadoSePropaga() {
        EnviosClient client = clienteQueResponde(HttpStatus.NOT_FOUND, "{\"error\":\"not_found\",\"message\":\"no existe\"}");

        assertThat(client.buscarPorId(UUID.randomUUID(), identidad)).isEmpty();
        assertThat(client.buscarPorPedido(UUID.randomUUID(), identidad)).isEmpty();
        assertThatThrownBy(() -> client.cambiarEstado(UUID.randomUUID(), new CambioEstadoEnvioDto("EN_TRANSITO", "x"), identidad))
                .isInstanceOfSatisfying(EnvioRechazoException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(404);
                    assertThat(ex.getCodigo()).isEqualTo("NO_ENCONTRADO");
                    assertThat(ex.getMessage()).isEqualTo("No se encontró el recurso solicitado");
                });
    }

    @Test
    void propagaUn400ConSuMensaje() {
        EnviosClient client = clienteQueResponde(HttpStatus.BAD_REQUEST, "{\"message\":\"datos invalidos\"}");

        assertThatThrownBy(() -> client.listarTodos(identidad))
                .isInstanceOfSatisfying(EnvioRechazoException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(400);
                    assertThat(ex.getCodigo()).isEqualTo("SOLICITUD_INVALIDA");
                    assertThat(ex.getMessage()).isEqualTo("La solicitud no es válida");
                });
    }

    @Test
    void unCodigoDesconocidoDeUn409SeInformaComoConflictoConMensajeFijo() {
        EnviosClient client = clienteQueResponde(HttpStatus.CONFLICT,
                "{\"error\":\"OTRO_CODIGO\",\"message\":\"texto interno con id 99\"}");

        assertThatThrownBy(() -> client.listarTodos(identidad))
                .isInstanceOfSatisfying(EnvioRechazoException.class, ex -> {
                    assertThat(ex.getCodigo()).isEqualTo("CONFLICTO");
                    assertThat(ex.getMessage()).isEqualTo("La operación entra en conflicto con el estado actual");
                });
    }

    @Test
    void noReenviaUn401NiUn403NiUnaFallaDelServidor() {
        assertThatThrownBy(() -> clienteQueResponde(HttpStatus.UNAUTHORIZED, "{}").listarTodos(identidad))
                .isInstanceOf(WebClientResponseException.class);
        assertThatThrownBy(() -> clienteQueResponde(HttpStatus.FORBIDDEN, "{}").listarTodos(identidad))
                .isInstanceOf(WebClientResponseException.class);
        assertThatThrownBy(() -> clienteQueResponde(HttpStatus.INTERNAL_SERVER_ERROR, "{}").listarTodos(identidad))
                .isInstanceOf(WebClientResponseException.class);
    }
}
