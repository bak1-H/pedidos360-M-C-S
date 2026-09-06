package com.pedidos360.bff.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Que un microservicio interno falle no puede convertirse en un 500 opaco:
 * lo traducimos a 502/504 para que Angular sepa que el problema esta abajo
 * y no en el token del usuario.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> microservicioRespondioError(
            WebClientResponseException ex, HttpServletRequest request) {

        log.error("Microservicio interno respondio {} en {}", ex.getStatusCode(), request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.de(502, "ERROR_MICROSERVICIO",
                        "Un servicio interno respondio con error " + ex.getStatusCode().value(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<ErrorResponse> microservicioInalcanzable(
            WebClientRequestException ex, HttpServletRequest request) {

        log.error("No se pudo contactar un microservicio interno desde {}", request.getRequestURI(), ex);

        return ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .body(ErrorResponse.de(504, "MICROSERVICIO_INALCANZABLE",
                        "No se pudo contactar un servicio interno", request.getRequestURI()));
    }
}
