package com.pedidos360.envios.event;

import com.pedidos360.envios.model.EstadoEnvio;

import java.time.LocalDateTime;
import java.util.UUID;

public record EnvioEstadoCambiadoEvent(
        UUID envioId,
        UUID pedidoId,
        EstadoEnvio estadoEnvio,
        String descripcion,
        LocalDateTime fecha
) {
}
