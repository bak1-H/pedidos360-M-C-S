package com.pedidos360.envios.event;

import com.pedidos360.envios.model.EstadoEnvio;

import java.util.UUID;

public record EnvioTrackingEvent(UUID envioId, UUID pedidoId, UUID repartidorId, EstadoEnvio estado) {
}
