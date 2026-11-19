package com.pedidos360.notificaciones.controller;

import com.pedidos360.notificaciones.dto.MetricasStreamingResponse;
import com.pedidos360.notificaciones.service.MetricasStreamingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notificaciones")
public class MetricasStreamingInternalController {

    private final MetricasStreamingService metricasStreamingService;

    public MetricasStreamingInternalController(MetricasStreamingService metricasStreamingService) {
        this.metricasStreamingService = metricasStreamingService;
    }

    @GetMapping("/metricas-streaming")
    public ResponseEntity<MetricasStreamingResponse> obtener() {
        return ResponseEntity.ok(metricasStreamingService.instantanea());
    }
}
