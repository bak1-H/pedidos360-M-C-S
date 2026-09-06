package com.pedidos360.usuarios.controller;

import com.pedidos360.usuarios.dto.ActualizarRolRequest;
import com.pedidos360.usuarios.dto.CrearUsuarioRequest;
import com.pedidos360.usuarios.dto.UsuarioResponse;
import com.pedidos360.usuarios.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Endpoints internos: NO se exponen a Angular. Solo el BFF los alcanza
 * (regla de red + header X-Internal-Token, ver InternalTokenFilter).
 */
@RestController
@RequestMapping("/internal/usuarios")
@RequiredArgsConstructor
public class UsuarioInternalController {

    private final UsuarioService usuarioService;

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{azureAdObjectId}")
    public UsuarioResponse obtenerPorAzureAdObjectId(@PathVariable String azureAdObjectId) {
        return usuarioService.buscarPorAzureAdObjectId(azureAdObjectId);
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        UsuarioResponse creado = usuarioService.crear(request);
        return ResponseEntity
                .created(URI.create("/internal/usuarios/" + creado.azureAdObjectId()))
                .body(creado);
    }

    @PatchMapping("/{id}/rol")
    public UsuarioResponse actualizarRol(@PathVariable UUID id,
                                         @Valid @RequestBody ActualizarRolRequest request) {
        return usuarioService.actualizarRol(id, request.rol());
    }
}
