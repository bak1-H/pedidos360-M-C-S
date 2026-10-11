package com.pedidos360.bff.client;

import com.pedidos360.bff.exception.NotificacionesRechazoException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificacionesClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA =
            new ParameterizedTypeReference<>() {
            };

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA =
            new ParameterizedTypeReference<>() {
            };

    private static final String MENSAJE_POR_DEFECTO = "El servicio de notificaciones rechazo la solicitud";

    private final WebClient notificacionesWebClient;

    public Map<String, Object> listar(String tipo, String estado, String pagina, String tamano) {
        return enviar(notificacionesWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/internal/notificaciones")
                        .queryParamIfPresent("tipo", Optional.ofNullable(tipo))
                        .queryParamIfPresent("estado", Optional.ofNullable(estado))
                        .queryParamIfPresent("pagina", Optional.ofNullable(pagina))
                        .queryParamIfPresent("tamano", Optional.ofNullable(tamano))
                        .build()))
                .bodyToMono(MAPA)
                .block();
    }

    public Map<String, Object> resumir() {
        return enviar(notificacionesWebClient.get()
                .uri("/internal/notificaciones/resumen"))
                .bodyToMono(MAPA)
                .block();
    }

    public List<Map<String, Object>> listarColas() {
        return enviar(notificacionesWebClient.get()
                .uri("/internal/notificaciones/colas"))
                .bodyToMono(LISTA)
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

    private Mono<NotificacionesRechazoException> rechazo(ClientResponse respuesta) {
        int status = respuesta.statusCode().value();
        return respuesta.bodyToMono(MAPA)
                .map(cuerpo -> String.valueOf(cuerpo.getOrDefault("message", MENSAJE_POR_DEFECTO)))
                .defaultIfEmpty(MENSAJE_POR_DEFECTO)
                .onErrorReturn(MENSAJE_POR_DEFECTO)
                .map(mensaje -> new NotificacionesRechazoException(status, mensaje));
    }
}
