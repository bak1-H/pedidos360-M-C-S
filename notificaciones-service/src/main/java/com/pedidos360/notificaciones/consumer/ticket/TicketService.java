package com.pedidos360.notificaciones.consumer.ticket;

import com.pedidos360.notificaciones.event.EnvioEntregadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final Path directorioSalida;

    public TicketService(@Value("${tickets.output-dir}") String directorioSalida) {
        this.directorioSalida = Path.of(directorioSalida);
    }

    public Path generar(EnvioEntregadoEvent event) {
        try {
            Files.createDirectories(directorioSalida);
            Path archivo = directorioSalida.resolve("ticket-" + event.envioId() + ".txt");
            Files.writeString(archivo, contenido(event));
            log.info("Ticket del envio {} generado en {}", event.envioId(), archivo.toAbsolutePath());
            return archivo;
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo escribir el ticket del envio " + event.envioId(), ex);
        }
    }

    private String contenido(EnvioEntregadoEvent event) {
        return String.join(System.lineSeparator(),
                "TICKET DE ENTREGA",
                "Envio: " + event.envioId(),
                "Pedido: " + event.pedidoId(),
                "Repartidor: " + event.repartidorId(),
                "Entregado el: " + event.fechaEntrega(),
                "");
    }
}
