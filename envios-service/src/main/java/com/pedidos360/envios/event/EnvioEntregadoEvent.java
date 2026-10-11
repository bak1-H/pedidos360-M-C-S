package com.pedidos360.envios.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record EnvioEntregadoEvent(
        UUID envioId,
        UUID pedidoId,
        UUID repartidorId,
        LocalDateTime fechaEntrega
) {
}
