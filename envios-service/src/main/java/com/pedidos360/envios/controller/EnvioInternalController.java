package com.pedidos360.envios.controller;

import com.pedidos360.envios.dto.CambioEstadoEnvioDTO;
import com.pedidos360.envios.dto.EnvioRequestDTO;
import com.pedidos360.envios.dto.EnvioResponseDTO;
import com.pedidos360.envios.service.EnvioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/envios")
public class EnvioInternalController {

    private final EnvioService envioService;

    public EnvioInternalController(EnvioService envioService) {
        this.envioService = envioService;
    }

    @PostMapping
    public ResponseEntity<EnvioResponseDTO> crear(@Valid @RequestBody EnvioRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(envioService.crear(request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<EnvioResponseDTO> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody CambioEstadoEnvioDTO cambio) {
        return ResponseEntity.ok(envioService.cambiarEstado(id, cambio));
    }

    @GetMapping("/repartidor/{repartidorId}")
    public ResponseEntity<List<EnvioResponseDTO>> buscarPorRepartidor(@PathVariable UUID repartidorId) {
        return ResponseEntity.ok(envioService.buscarPorRepartidor(repartidorId));
    }

    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<EnvioResponseDTO> buscarPorPedido(@PathVariable UUID pedidoId) {
        return ResponseEntity.ok(envioService.buscarPorPedido(pedidoId));
    }
}
