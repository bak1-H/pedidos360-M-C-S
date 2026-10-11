package com.pedidos360.rabbitadmin.controller;

import com.pedidos360.rabbitadmin.dto.BindingRequest;
import com.pedidos360.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/bindings")
public class BindingAdminController {

    private final RabbitAdminService rabbitAdminService;

    public BindingAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping
    public ResponseEntity<BindingRequest> crear(@Valid @RequestBody BindingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rabbitAdminService.crearBinding(request));
    }

    @DeleteMapping
    public ResponseEntity<Void> eliminar(@Valid @RequestBody BindingRequest request) {
        rabbitAdminService.eliminarBinding(request);
        return ResponseEntity.noContent().build();
    }
}
