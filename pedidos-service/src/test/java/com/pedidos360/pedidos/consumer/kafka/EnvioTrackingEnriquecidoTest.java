package com.pedidos360.pedidos.consumer.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EnvioTrackingEnriquecidoTest {

    @Test
    void deserializaUnTrackingEnriquecidoIgnorandoLosCamposQueNoDeclara() {
        String json = """
                {"eventoId":"e1","envioId":"%s","pedidoId":"%s","repartidorId":null,"estado":"EN_TRANSITO",
                 "ocurridoEn":"2026-01-01T10:00:00Z","descripcion":"Salio a reparto",
                 "fechaAsignacion":"2026-01-01T09:00:00","fechaEntregaEstimada":"2026-01-02T09:00:00"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        try (JsonDeserializer<EnvioTrackingEvento> deserializador = new JsonDeserializer<>(EnvioTrackingEvento.class, false)) {
            EnvioTrackingEvento evento = deserializador.deserialize("topico", json.getBytes(StandardCharsets.UTF_8));

            assertThat(evento.estado()).isEqualTo("EN_TRANSITO");
        }
    }
}
