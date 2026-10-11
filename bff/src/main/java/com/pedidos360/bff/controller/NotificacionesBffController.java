package com.pedidos360.bff.controller;

import com.pedidos360.bff.client.NotificacionesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bff/notificaciones")
@RequiredArgsConstructor
public class NotificacionesBffController {

    private final NotificacionesClient notificacionesClient;

    @GetMapping
    public Map<String, Object> listar(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String pagina,
            @RequestParam(required = false) String tamano) {
        return notificacionesClient.listar(tipo, estado, pagina, tamano);
    }

    @GetMapping("/resumen")
    public Map<String, Object> resumir() {
        return notificacionesClient.resumir();
    }

    @GetMapping("/colas")
    public List<Map<String, Object>> listarColas() {
        return notificacionesClient.listarColas();
    }
}
