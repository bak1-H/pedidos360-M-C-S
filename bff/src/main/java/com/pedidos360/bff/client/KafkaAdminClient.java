package com.pedidos360.bff.client;

import com.pedidos360.bff.exception.KafkaAdminRechazoException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class KafkaAdminClient {

    private static final ParameterizedTypeReference<Object> JSON =
            new ParameterizedTypeReference<>() {
            };

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA =
            new ParameterizedTypeReference<>() {
            };

    private static final String MENSAJE_POR_DEFECTO = "El servicio de Kafka rechazo la solicitud";

    private final WebClient kafkaAdminWebClient;

    public Object consultar(String uri, Object... variables) {
        return enviar(kafkaAdminWebClient.get().uri(uri, variables))
                .bodyToMono(JSON)
                .block();
    }

    public Object crear(String uri, Map<String, Object> cuerpo, Object... variables) {
        return enviar(kafkaAdminWebClient.post().uri(uri, variables).bodyValue(cuerpo))
                .bodyToMono(JSON)
                .block();
    }

    public Object actualizar(String uri, Map<String, Object> cuerpo, Object... variables) {
        return enviar(kafkaAdminWebClient.put().uri(uri, variables).bodyValue(cuerpo))
                .bodyToMono(JSON)
                .block();
    }

    public void eliminar(String uri, Object... variables) {
        enviar(kafkaAdminWebClient.delete().uri(uri, variables))
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

    private Mono<KafkaAdminRechazoException> rechazo(ClientResponse respuesta) {
        int status = respuesta.statusCode().value();
        return respuesta.bodyToMono(MAPA)
                .map(cuerpo -> String.valueOf(cuerpo.getOrDefault("message", MENSAJE_POR_DEFECTO)))
                .defaultIfEmpty(MENSAJE_POR_DEFECTO)
                .onErrorReturn(MENSAJE_POR_DEFECTO)
                .map(mensaje -> new KafkaAdminRechazoException(status, mensaje));
    }
}
