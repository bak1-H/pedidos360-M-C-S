package com.pedidos360.notificaciones.controller;

import com.pedidos360.notificaciones.dto.ColaResponse;
import com.pedidos360.notificaciones.dto.NotificacionResponse;
import com.pedidos360.notificaciones.dto.PaginaResponse;
import com.pedidos360.notificaciones.dto.ResumenResponse;
import com.pedidos360.notificaciones.model.EstadoNotificacion;
import com.pedidos360.notificaciones.model.TipoNotificacion;
import com.pedidos360.notificaciones.service.ColaService;
import com.pedidos360.notificaciones.service.NotificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/notificaciones")
public class NotificacionInternalController {

    private final NotificacionService notificacionService;
    private final ColaService colaService;

    public NotificacionInternalController(NotificacionService notificacionService, ColaService colaService) {
        this.notificacionService = notificacionService;
        this.colaService = colaService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<NotificacionResponse>> listar(
            @RequestParam(required = false) TipoNotificacion tipo,
            @RequestParam(required = false) EstadoNotificacion estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return ResponseEntity.ok(notificacionService.listar(tipo, estado, pagina, tamano));
    }

    @GetMapping("/resumen")
    public ResponseEntity<ResumenResponse> resumir() {
        return ResponseEntity.ok(notificacionService.resumir());
    }

    @GetMapping("/colas")
    public ResponseEntity<List<ColaResponse>> listarColas() {
        return ResponseEntity.ok(colaService.listar());
    }
}
