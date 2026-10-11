package com.pedidos360.notificaciones.consumer.ticket;

import com.pedidos360.notificaciones.event.EnvioEntregadoEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketServiceTest {

    @TempDir
    Path directorio;

    @Test
    void generaElArchivoDelTicketConLosDatosDelEnvio() throws IOException {
        TicketService service = new TicketService(directorio.resolve("tickets").toString());
        UUID envioId = UUID.randomUUID();
        EnvioEntregadoEvent event = new EnvioEntregadoEvent(envioId, UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now());

        Path archivo = service.generar(event);

        assertThat(archivo.getFileName().toString()).isEqualTo("ticket-" + envioId + ".txt");
        assertThat(Files.readString(archivo)).contains("TICKET DE ENTREGA").contains(envioId.toString());
    }
}
