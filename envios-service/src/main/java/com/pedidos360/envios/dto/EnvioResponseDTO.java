package com.pedidos360.envios.dto;

import com.pedidos360.envios.model.EstadoEnvio;
import com.pedidos360.envios.model.Envio;

import java.time.LocalDateTime;
import java.util.UUID;

public record EnvioResponseDTO(
        UUID id,
        UUID pedidoId,
        UUID repartidorId,
        EstadoEnvio estadoEnvio,
        LocalDateTime fechaAsignacion,
        LocalDateTime fechaEntregaEstimada
) {

    public static EnvioResponseDTO from(Envio envio) {
        return new EnvioResponseDTO(
                envio.getId(),
                envio.getPedidoId(),
                envio.getRepartidorId(),
                envio.getEstadoEnvio(),
                envio.getFechaAsignacion(),
                envio.getFechaEntregaEstimada()
        );
    }
}
