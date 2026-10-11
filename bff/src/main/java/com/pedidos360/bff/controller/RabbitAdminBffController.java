package com.pedidos360.bff.controller;

import com.pedidos360.bff.client.RabbitAdminClient;
import com.pedidos360.bff.dto.BindingRequest;
import com.pedidos360.bff.dto.CrearColaRequest;
import com.pedidos360.bff.dto.CrearExchangeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/bff/rabbit")
@RequiredArgsConstructor
public class RabbitAdminBffController {

    private final RabbitAdminClient rabbitAdminClient;

    @PostMapping("/queues")
    public ResponseEntity<Map<String, Object>> crearCola(@Valid @RequestBody CrearColaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rabbitAdminClient.crearCola(request));
    }

    @GetMapping("/queues/{nombre}")
    public Map<String, Object> obtenerCola(@PathVariable String nombre) {
        return rabbitAdminClient.obtenerCola(nombre);
    }

    @DeleteMapping("/queues/{nombre}")
    public ResponseEntity<Void> eliminarCola(@PathVariable String nombre) {
        rabbitAdminClient.eliminarCola(nombre);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, Object>> crearExchange(@Valid @RequestBody CrearExchangeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rabbitAdminClient.crearExchange(request));
    }

    @DeleteMapping("/exchanges/{nombre}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable String nombre) {
        rabbitAdminClient.eliminarExchange(nombre);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bindings")
    public ResponseEntity<Map<String, Object>> crearBinding(@Valid @RequestBody BindingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rabbitAdminClient.crearBinding(request));
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@Valid @RequestBody BindingRequest request) {
        rabbitAdminClient.eliminarBinding(request);
        return ResponseEntity.noContent().build();
    }
}
