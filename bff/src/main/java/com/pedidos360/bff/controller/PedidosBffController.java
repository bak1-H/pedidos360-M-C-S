package com.pedidos360.bff.controller;

import com.pedidos360.bff.client.EnviosClient;
import com.pedidos360.bff.client.PedidosClient;
import com.pedidos360.bff.client.UsuariosClient;
import com.pedidos360.bff.dto.CrearPedidoRequest;
import com.pedidos360.bff.dto.PedidoConTrackingResponse;
import com.pedidos360.bff.dto.PedidoRequestDto;
import com.pedidos360.bff.dto.PedidoResponseDto;
import com.pedidos360.bff.exception.AccesoNoAutorizadoException;
import com.pedidos360.bff.exception.RecursoNoEncontradoException;
import com.pedidos360.bff.security.IdentidadInterna;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * SDD seccion 4.1: los unicos endpoints de pedidos que Angular conoce.
 * pedidos-service y envios-service no son publicos, solo el BFF los llama.
 */
@RestController
@RequestMapping("/bff/pedidos")
@RequiredArgsConstructor
public class PedidosBffController {

    private final PedidosClient pedidosClient;
    private final EnviosClient enviosClient;
    private final UsuariosClient usuariosClient;

    @PostMapping
    public ResponseEntity<PedidoResponseDto> crear(
            @Valid @RequestBody CrearPedidoRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        IdentidadInterna identidad = IdentidadInterna.desde(jwt);
        UUID clienteId = usuariosClient.resolverUsuarioInternoId(identidad);

        PedidoResponseDto creado = pedidosClient.crear(PedidoRequestDto.desde(clienteId, request), identidad);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/mios")
    public List<PedidoResponseDto> misPedidos(@AuthenticationPrincipal Jwt jwt) {
        IdentidadInterna identidad = IdentidadInterna.desde(jwt);
        UUID clienteId = usuariosClient.resolverUsuarioInternoId(identidad);
        return pedidosClient.listarPorCliente(clienteId, identidad);
    }

    @GetMapping("/{id}")
    public PedidoConTrackingResponse buscarPorId(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        IdentidadInterna identidad = IdentidadInterna.desde(jwt);

        PedidoResponseDto pedido = pedidosClient.buscarPorId(id, identidad)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado: " + id));

        if (!identidad.esAdmin()) {
            UUID clienteId = usuariosClient.resolverUsuarioInternoId(identidad);
            if (!clienteId.equals(pedido.clienteId())) {
                throw new AccesoNoAutorizadoException("El pedido no pertenece al usuario autenticado");
            }
        }

        return new PedidoConTrackingResponse(
                pedido,
                enviosClient.buscarPorPedido(id, identidad).orElse(null));
    }

    @GetMapping
    public List<PedidoResponseDto> listarTodos(@AuthenticationPrincipal Jwt jwt) {
        return pedidosClient.listarTodos(IdentidadInterna.desde(jwt));
    }
}
