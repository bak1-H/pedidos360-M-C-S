package com.pedidos360.pedidos.controller;

import com.pedidos360.pedidos.consumer.kafka.EnvioTrackingEvento;
import com.pedidos360.pedidos.service.TrackingService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/pedidos")
public class TrackingInternalController {

    private final TrackingService trackingService;

    public TrackingInternalController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<EnvioTrackingEvento> obtenerTracking(@PathVariable UUID id) {
        return ResponseEntity.ok(trackingService.buscar(id)
                .orElseThrow(() -> new EntityNotFoundException("Sin tracking para el pedido: " + id)));
    }
}
