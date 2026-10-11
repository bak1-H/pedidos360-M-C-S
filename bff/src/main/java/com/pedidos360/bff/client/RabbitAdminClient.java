package com.pedidos360.bff.client;

import com.pedidos360.bff.dto.BindingRequest;
import com.pedidos360.bff.dto.CrearColaRequest;
import com.pedidos360.bff.dto.CrearExchangeRequest;
import com.pedidos360.bff.exception.RabbitAdminRechazoException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RabbitAdminClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA =
            new ParameterizedTypeReference<>() {
            };

    private static final String MENSAJE_POR_DEFECTO = "El servicio de RabbitMQ rechazo la solicitud";

    private final WebClient rabbitAdminWebClient;

    public Map<String, Object> crearCola(CrearColaRequest request) {
        return enviar(rabbitAdminWebClient.post()
                .uri("/internal/queues")
                .bodyValue(request))
                .bodyToMono(MAPA)
                .block();
    }

    public Map<String, Object> obtenerCola(String nombre) {
        return enviar(rabbitAdminWebClient.get()
                .uri("/internal/queues/{nombre}", nombre))
                .bodyToMono(MAPA)
                .block();
    }

    public void eliminarCola(String nombre) {
        enviar(rabbitAdminWebClient.delete()
                .uri("/internal/queues/{nombre}", nombre))
                .toBodilessEntity()
                .block();
    }

    public Map<String, Object> crearExchange(CrearExchangeRequest request) {
        return enviar(rabbitAdminWebClient.post()
                .uri("/internal/exchanges")
                .bodyValue(request))
                .bodyToMono(MAPA)
                .block();
    }

    public void eliminarExchange(String nombre) {
        enviar(rabbitAdminWebClient.delete()
                .uri("/internal/exchanges/{nombre}", nombre))
                .toBodilessEntity()
                .block();
    }

    public Map<String, Object> crearBinding(BindingRequest request) {
        return enviar(rabbitAdminWebClient.post()
                .uri("/internal/bindings")
                .bodyValue(request))
                .bodyToMono(MAPA)
                .block();
    }

    public void eliminarBinding(BindingRequest request) {
        enviar(rabbitAdminWebClient.method(HttpMethod.DELETE)
                .uri("/internal/bindings")
                .bodyValue(request))
                .toBodilessEntity()
                .block();
    }

    private WebClient.ResponseSpec enviar(WebClient.RequestHeadersSpec<?> solicitud) {
        return solicitud.retrieve()
                .onStatus(this::esRechazoReenviable, this::rechazo);
    }

    private boolean esRechazoReenviable(HttpStatusCode status) {
        return status.is4xxClientError()
                && status.value() != HttpStatus.UNAUTHORIZED.value()
                && status.value() != HttpStatus.FORBIDDEN.value();
    }

    private Mono<RabbitAdminRechazoException> rechazo(ClientResponse respuesta) {
        int status = respuesta.statusCode().value();
        return respuesta.bodyToMono(MAPA)
                .map(cuerpo -> String.valueOf(cuerpo.getOrDefault("message", MENSAJE_POR_DEFECTO)))
                .defaultIfEmpty(MENSAJE_POR_DEFECTO)
                .onErrorReturn(MENSAJE_POR_DEFECTO)
                .map(mensaje -> new RabbitAdminRechazoException(status, mensaje));
    }
}
