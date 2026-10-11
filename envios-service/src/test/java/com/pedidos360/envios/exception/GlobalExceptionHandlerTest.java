package com.pedidos360.envios.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private ResponseEntity<Map<String, String>> manejar(SQLException causa) {
        return handler.handleIntegridad(new DataIntegrityViolationException("integridad", causa));
    }

    @Test
    void unaViolacionDeUnicidadSeInformaComoEnvioDuplicado() {
        ResponseEntity<Map<String, String>> postgres = manejar(new SQLException("duplicate key", "23505", 0));
        ResponseEntity<Map<String, String>> h2 = manejar(new SQLException("Unique index", "23505", 23505));
        ResponseEntity<Map<String, String>> sqlServer = manejar(new SQLException("Violation of UNIQUE KEY", "23000", 2627));

        for (ResponseEntity<Map<String, String>> respuesta : List.of(postgres, h2, sqlServer)) {
            assertThat(respuesta.getStatusCode().value()).isEqualTo(409);
            assertThat(respuesta.getBody()).containsEntry("error", "ENVIO_DUPLICADO");
        }
    }

    @Test
    void otraViolacionDeIntegridadSeInformaComoSolicitudInvalida() {
        ResponseEntity<Map<String, String>> notNull = manejar(new SQLException("null value", "23502", 0));
        ResponseEntity<Map<String, String>> sinCausa = handler.handleIntegridad(new DataIntegrityViolationException("x"));

        for (ResponseEntity<Map<String, String>> respuesta : List.of(notNull, sinCausa)) {
            assertThat(respuesta.getStatusCode().value()).isEqualTo(400);
            assertThat(respuesta.getBody()).containsEntry("error", "SOLICITUD_INVALIDA");
        }
    }
}
