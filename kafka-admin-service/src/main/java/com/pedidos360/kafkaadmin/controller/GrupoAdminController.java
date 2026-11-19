package com.pedidos360.kafkaadmin.controller;

import com.pedidos360.kafkaadmin.dto.GrupoDetalleResponse;
import com.pedidos360.kafkaadmin.dto.GrupoResumenResponse;
import com.pedidos360.kafkaadmin.dto.OffsetParticionResponse;
import com.pedidos360.kafkaadmin.dto.ResetOffsetsRequest;
import com.pedidos360.kafkaadmin.service.GrupoAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/kafka-admin/grupos")
public class GrupoAdminController {

    private final GrupoAdminService grupoAdminService;

    public GrupoAdminController(GrupoAdminService grupoAdminService) {
        this.grupoAdminService = grupoAdminService;
    }

    @GetMapping
    public ResponseEntity<List<GrupoResumenResponse>> listar() {
        return ResponseEntity.ok(grupoAdminService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GrupoDetalleResponse> obtener(@PathVariable String id) {
        return ResponseEntity.ok(grupoAdminService.obtener(id));
    }

    @PostMapping("/{id}/reset-offsets")
    public ResponseEntity<List<OffsetParticionResponse>> resetearOffsets(
            @PathVariable String id, @Valid @RequestBody ResetOffsetsRequest request) {
        return ResponseEntity.ok(grupoAdminService.resetearOffsets(id, request));
    }
}
