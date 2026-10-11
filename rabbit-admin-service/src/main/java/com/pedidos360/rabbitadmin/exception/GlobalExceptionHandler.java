package com.pedidos360.rabbitadmin.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return badRequest(detalle);
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<Map<String, String>> handleSolicitudInvalida(SolicitudInvalidaException ex) {
        return badRequest(ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleBodyIlegible(HttpMessageNotReadableException ex) {
        return badRequest("El cuerpo de la solicitud falta o no es un JSON valido");
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "not_found", "message", ex.getMessage()));
    }

    @ExceptionHandler(RecursoProtegidoException.class)
    public ResponseEntity<Map<String, String>> handleProtegido(RecursoProtegidoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "protected_resource", "message", ex.getMessage()));
    }

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<Map<String, String>> handleBroker(AmqpException ex) {
        log.error("RabbitMQ rechazo la operacion: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "broker_error", "message", "RabbitMQ no pudo completar la operacion"));
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "bad_request", "message", message));
    }
}
