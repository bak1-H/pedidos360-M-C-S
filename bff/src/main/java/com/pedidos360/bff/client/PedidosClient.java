package com.pedidos360.bff.client;

import com.pedidos360.bff.dto.PedidoRequestDto;
import com.pedidos360.bff.dto.PedidoResponseDto;
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
public class PedidosClient {

    private final WebClient pedidosWebClient;

    public PedidoResponseDto crear(PedidoRequestDto request, IdentidadInterna identidad) {
        return pedidosWebClient.post()
                .uri("/internal/pedidos")
                .headers(identidad::aplicar)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PedidoResponseDto.class)
                .block();
    }

    public Optional<PedidoResponseDto> buscarPorId(UUID id, IdentidadInterna identidad) {
        try {
            PedidoResponseDto pedido = pedidosWebClient.get()
                    .uri("/internal/pedidos/{id}", id)
                    .headers(identidad::aplicar)
                    .retrieve()
                    .bodyToMono(PedidoResponseDto.class)
                    .block();
            return Optional.ofNullable(pedido);

        } catch (WebClientResponseException.NotFound e) {
            return Optional.empty();
        }
    }

    public List<PedidoResponseDto> listarPorCliente(UUID clienteId, IdentidadInterna identidad) {
        return pedidosWebClient.get()
                .uri("/internal/pedidos/cliente/{clienteId}", clienteId)
                .headers(identidad::aplicar)
                .retrieve()
                .bodyToFlux(PedidoResponseDto.class)
                .collectList()
                .block();
    }

    public List<PedidoResponseDto> listarTodos(IdentidadInterna identidad) {
        return pedidosWebClient.get()
                .uri("/internal/pedidos")
                .headers(identidad::aplicar)
                .retrieve()
                .bodyToFlux(PedidoResponseDto.class)
                .collectList()
                .block();
    }
}
