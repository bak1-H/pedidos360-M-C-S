package com.pedidos360.kafkaadmin.controller;

import com.pedidos360.kafkaadmin.dto.AumentarParticionesRequest;
import com.pedidos360.kafkaadmin.dto.CambiarConfiguracionRequest;
import com.pedidos360.kafkaadmin.dto.CrearTopicoRequest;
import com.pedidos360.kafkaadmin.dto.TopicoDetalleResponse;
import com.pedidos360.kafkaadmin.dto.TopicoResumenResponse;
import com.pedidos360.kafkaadmin.service.TopicoAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/kafka-admin/topicos")
public class TopicoAdminController {

    private final TopicoAdminService topicoAdminService;

    public TopicoAdminController(TopicoAdminService topicoAdminService) {
        this.topicoAdminService = topicoAdminService;
    }

    @GetMapping
    public ResponseEntity<List<TopicoResumenResponse>> listar() {
        return ResponseEntity.ok(topicoAdminService.listar());
    }

    @GetMapping("/{nombre}")
    public ResponseEntity<TopicoDetalleResponse> obtener(@PathVariable String nombre) {
        return ResponseEntity.ok(topicoAdminService.obtener(nombre));
    }

    @PostMapping
    public ResponseEntity<TopicoResumenResponse> crear(@Valid @RequestBody CrearTopicoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(topicoAdminService.crear(request));
    }

    @PutMapping("/{nombre}/configuracion")
    public ResponseEntity<TopicoDetalleResponse> cambiarConfiguracion(
            @PathVariable String nombre, @RequestBody CambiarConfiguracionRequest request) {
        return ResponseEntity.ok(topicoAdminService.cambiarConfiguracion(nombre, request));
    }

    @PostMapping("/{nombre}/particiones")
    public ResponseEntity<TopicoDetalleResponse> aumentarParticiones(
            @PathVariable String nombre, @Valid @RequestBody AumentarParticionesRequest request) {
        return ResponseEntity.ok(topicoAdminService.aumentarParticiones(nombre, request));
    }

    @DeleteMapping("/{nombre}")
    public ResponseEntity<Void> eliminar(@PathVariable String nombre) {
        topicoAdminService.eliminar(nombre);
        return ResponseEntity.noContent().build();
    }
}
