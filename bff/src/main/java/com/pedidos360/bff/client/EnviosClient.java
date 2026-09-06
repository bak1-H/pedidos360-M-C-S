package com.pedidos360.bff.client;

import com.pedidos360.bff.dto.CambioEstadoEnvioDto;
import com.pedidos360.bff.dto.EnvioRequestDto;
import com.pedidos360.bff.dto.EnvioResponseDto;
import com.pedidos360.bff.security.IdentidadInterna;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EnviosClient {

    private final WebClient enviosWebClient;

    public EnvioResponseDto crear(EnvioRequestDto request, IdentidadInterna identidad) {
        return enviosWebClient.post()
                .uri("/internal/envios")
                .headers(identidad::aplicar)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(EnvioResponseDto.class)
                .block();
    }

    public Optional<EnvioResponseDto> buscarPorId(UUID id, IdentidadInterna identidad) {
        try {
            EnvioResponseDto envio = enviosWebClient.get()
                    .uri("/internal/envios/{id}", id)
                    .headers(identidad::aplicar)
                    .retrieve()
                    .bodyToMono(EnvioResponseDto.class)
                    .block();
            return Optional.ofNullable(envio);

        } catch (WebClientResponseException.NotFound e) {
            return Optional.empty();
        }
    }

    public Optional<EnvioResponseDto> buscarPorPedido(UUID pedidoId, IdentidadInterna identidad) {
        try {
            EnvioResponseDto envio = enviosWebClient.get()
                    .uri("/internal/envios/pedido/{pedidoId}", pedidoId)
                    .headers(identidad::aplicar)
                    .retrieve()
                    .bodyToMono(EnvioResponseDto.class)
                    .block();
            return Optional.ofNullable(envio);

        } catch (WebClientResponseException.NotFound e) {
            return Optional.empty();
        }
    }

    public List<EnvioResponseDto> buscarPorRepartidor(UUID repartidorId, IdentidadInterna identidad) {
        return enviosWebClient.get()
                .uri("/internal/envios/repartidor/{repartidorId}", repartidorId)
                .headers(identidad::aplicar)
                .retrieve()
                .bodyToFlux(EnvioResponseDto.class)
                .collectList()
                .block();
    }

    public EnvioResponseDto cambiarEstado(UUID id, CambioEstadoEnvioDto cambio, IdentidadInterna identidad) {
        return enviosWebClient.patch()
                .uri("/internal/envios/{id}/estado", id)
                .headers(identidad::aplicar)
                .bodyValue(cambio)
                .retrieve()
                .bodyToMono(EnvioResponseDto.class)
                .block();
    }
}
