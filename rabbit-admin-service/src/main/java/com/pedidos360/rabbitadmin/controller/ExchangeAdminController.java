package com.pedidos360.rabbitadmin.controller;

import com.pedidos360.rabbitadmin.dto.CrearExchangeRequest;
import com.pedidos360.rabbitadmin.dto.ExchangeResponse;
import com.pedidos360.rabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/exchanges")
public class ExchangeAdminController {

    private final RabbitAdminService rabbitAdminService;

    public ExchangeAdminController(RabbitAdminService rabbitAdminService) {
        this.rabbitAdminService = rabbitAdminService;
    }

    @PostMapping
    public ResponseEntity<ExchangeResponse> crear(@Valid @RequestBody CrearExchangeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rabbitAdminService.crearExchange(request));
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Void> eliminar(@PathVariable String nombre) {
        rabbitAdminService.eliminarExchange(nombre);
        return ResponseEntity.noContent().build();
    }
}
