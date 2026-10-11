package com.pedidos360.bff.client;

import com.pedidos360.bff.dto.CambioEstadoEnvioDto;
import com.pedidos360.bff.dto.EnvioRequestDto;
import com.pedidos360.bff.dto.EnvioResponseDto;
import com.pedidos360.bff.exception.EnvioRechazoException;
import com.pedidos360.bff.security.IdentidadInterna;
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
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EnviosClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAPA =
            new ParameterizedTypeReference<>() {
            };

    private static final Set<String> CODIGOS_CONOCIDOS = Set.of("ENVIO_DUPLICADO", "TRANSICION_INVALIDA");

    private final WebClient enviosWebClient;

    public EnvioResponseDto crear(EnvioRequestDto request, IdentidadInterna identidad) {
        return enviar(enviosWebClient.post()
                .uri("/internal/envios")
                .headers(identidad::aplicar)
                .bodyValue(request))
                .bodyToMono(EnvioResponseDto.class)
                .block();
    }

    public List<EnvioResponseDto> listarTodos(IdentidadInterna identidad) {
        return enviar(enviosWebClient.get()
                .uri("/internal/envios")
                .headers(identidad::aplicar))
                .bodyToFlux(EnvioResponseDto.class)
                .collectList()
                .block();
    }

    public Optional<EnvioResponseDto> buscarPorId(UUID id, IdentidadInterna identidad) {
        return buscarOpcional(enviosWebClient.get()
                .uri("/internal/envios/{id}", id)
                .headers(identidad::aplicar));
    }

    public Optional<EnvioResponseDto> buscarPorPedido(UUID pedidoId, IdentidadInterna identidad) {
        return buscarOpcional(enviosWebClient.get()
                .uri("/internal/envios/pedido/{pedidoId}", pedidoId)
                .headers(identidad::aplicar));
    }

    public List<EnvioResponseDto> buscarPorRepartidor(UUID repartidorId, IdentidadInterna identidad) {
        return enviar(enviosWebClient.get()
                .uri("/internal/envios/repartidor/{repartidorId}", repartidorId)
                .headers(identidad::aplicar))
                .bodyToFlux(EnvioResponseDto.class)
                .collectList()
                .block();
    }

    public EnvioResponseDto cambiarEstado(UUID id, CambioEstadoEnvioDto cambio, IdentidadInterna identidad) {
        return enviar(enviosWebClient.patch()
                .uri("/internal/envios/{id}/estado", id)
                .headers(identidad::aplicar)
                .bodyValue(cambio))
                .bodyToMono(EnvioResponseDto.class)
                .block();
    }

    private Optional<EnvioResponseDto> buscarOpcional(WebClient.RequestHeadersSpec<?> solicitud) {
        try {
            return Optional.ofNullable(enviar(solicitud).bodyToMono(EnvioResponseDto.class).block());
        } catch (EnvioRechazoException e) {
            if (e.getStatus() == HttpStatus.NOT_FOUND.value()) {
                return Optional.empty();
            }
            throw e;
        }
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

    private Mono<EnvioRechazoException> rechazo(ClientResponse respuesta) {
        int status = respuesta.statusCode().value();
        return respuesta.bodyToMono(MAPA)
                .defaultIfEmpty(Map.of())
                .onErrorReturn(Map.of())
                .map(cuerpo -> {
                    String codigo = codigoDe(status, cuerpo.get("error"));
                    return new EnvioRechazoException(status, codigo, mensajeDe(codigo));
                });
    }

    private String mensajeDe(String codigo) {
        return switch (codigo) {
            case "ENVIO_DUPLICADO" -> "Ya existe un envío para el pedido indicado";
            case "TRANSICION_INVALIDA" -> "El envío no admite ese cambio de estado";
            case "NO_ENCONTRADO" -> "No se encontró el recurso solicitado";
            case "CONFLICTO" -> "La operación entra en conflicto con el estado actual";
            default -> "La solicitud no es válida";
        };
    }

    private String codigoDe(int status, Object error) {
        if (error instanceof String texto && CODIGOS_CONOCIDOS.contains(texto)) {
            return texto;
        }
        return switch (status) {
            case 404 -> "NO_ENCONTRADO";
            case 409 -> "CONFLICTO";
            default -> "SOLICITUD_INVALIDA";
        };
    }
}
