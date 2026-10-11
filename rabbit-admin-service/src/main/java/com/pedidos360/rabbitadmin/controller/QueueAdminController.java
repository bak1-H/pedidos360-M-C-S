package com.pedidos360.rabbitadmin.controller;

import com.pedidos360.rabbitadmin.dto.ColaInfoResponse;
import com.pedidos360.rabbitadmin.dto.ColaResponse;
import com.pedidos360.rabbitadmin.dto.CrearColaRequest;
import com.pedidos360.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/queues")
public class QueueAdminController {

    private final RabbitAdminService rabbitAdminService;

    public QueueAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping
    public ResponseEntity<ColaResponse> crear(@Valid @RequestBody CrearColaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rabbitAdminService.crearCola(request));
    }

    @GetMapping("/{nombre}")
    public ResponseEntity<ColaInfoResponse> obtener(@PathVariable String nombre) {
        return ResponseEntity.ok(rabbitAdminService.obtenerCola(nombre));
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Void> eliminar(@PathVariable String nombre) {
        rabbitAdminService.eliminarCola(nombre);
        return ResponseEntity.noContent().build();
    }
}
