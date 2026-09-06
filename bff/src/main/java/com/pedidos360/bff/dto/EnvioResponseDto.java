package com.pedidos360.bff.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Espejo de lo que devuelve envios-service en /internal/envios. */
public record EnvioResponseDto(
        UUID id,
        UUID pedidoId,
        UUID repartidorId,
        String estadoEnvio,
        LocalDateTime fechaAsignacion,
        LocalDateTime fechaEntregaEstimada) {
}
