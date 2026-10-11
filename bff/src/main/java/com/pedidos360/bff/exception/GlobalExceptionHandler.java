package com.pedidos360.bff.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Traduce fallas de los microservicios a 502/504 para no confundirlas con un error de token. */
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

    @ExceptionHandler(EnvioRechazoException.class)
    public ResponseEntity<ErrorResponse> envioRechazo(EnvioRechazoException ex, HttpServletRequest request) {

        return ResponseEntity
                .status(ex.getStatus())
                .body(ErrorResponse.de(ex.getStatus(), ex.getCodigo(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(PedidoNoAsignableException.class)
    public ResponseEntity<ErrorResponse> pedidoNoAsignable(PedidoNoAsignableException ex, HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.de(409, "PEDIDO_NO_ASIGNABLE", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(RabbitAdminRechazoException.class)
    public ResponseEntity<ErrorResponse> rabbitAdminRechazo(
            RabbitAdminRechazoException ex, HttpServletRequest request) {

        return ResponseEntity
                .status(ex.getStatus())
                .body(ErrorResponse.de(ex.getStatus(), "RABBIT_ADMIN_RECHAZO", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(NotificacionesRechazoException.class)
    public ResponseEntity<ErrorResponse> notificacionesRechazo(
            NotificacionesRechazoException ex, HttpServletRequest request) {

        return ResponseEntity
                .status(ex.getStatus())
                .body(ErrorResponse.de(ex.getStatus(), "NOTIFICACIONES_RECHAZO", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> recursoNoEncontrado(
            RecursoNoEncontradoException ex, HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.de(404, "NO_ENCONTRADO", ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(AccesoNoAutorizadoException.class)
    public ResponseEntity<ErrorResponse> accesoNoAutorizado(
            AccesoNoAutorizadoException ex, HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.de(403, "SIN_PERMISO", ex.getMessage(), request.getRequestURI()));
    }
}
