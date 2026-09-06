package com.pedidos360.bff.controller;

import com.pedidos360.bff.client.EnviosClient;
import com.pedidos360.bff.client.UsuariosClient;
import com.pedidos360.bff.dto.CambioEstadoEnvioDto;
import com.pedidos360.bff.dto.EnvioRequestDto;
import com.pedidos360.bff.dto.EnvioResponseDto;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** SDD seccion 4.1: los unicos endpoints de envios que Angular conoce. */
@RestController
@RequestMapping("/bff/envios")
@RequiredArgsConstructor
public class EnviosBffController {

    private final EnviosClient enviosClient;
    private final UsuariosClient usuariosClient;

    @PostMapping
    public ResponseEntity<EnvioResponseDto> crear(
            @Valid @RequestBody EnvioRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {

        EnvioResponseDto creado = enviosClient.crear(request, IdentidadInterna.desde(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PatchMapping("/{id}/estado")
    public EnvioResponseDto cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody CambioEstadoEnvioDto cambio,
            @AuthenticationPrincipal Jwt jwt) {

        IdentidadInterna identidad = IdentidadInterna.desde(jwt);

        if (!identidad.esAdmin()) {
            EnvioResponseDto envio = enviosClient.buscarPorId(id, identidad)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Envio no encontrado: " + id));

            UUID repartidorId = usuariosClient.resolverUsuarioInternoId(identidad);
            if (!repartidorId.equals(envio.repartidorId())) {
                throw new AccesoNoAutorizadoException("El envio no esta asignado al repartidor autenticado");
            }
        }

        return enviosClient.cambiarEstado(id, cambio, identidad);
    }

    @GetMapping("/mios")
    public List<EnvioResponseDto> misEnvios(@AuthenticationPrincipal Jwt jwt) {
        IdentidadInterna identidad = IdentidadInterna.desde(jwt);
        UUID repartidorId = usuariosClient.resolverUsuarioInternoId(identidad);
        return enviosClient.buscarPorRepartidor(repartidorId, identidad);
    }
}
