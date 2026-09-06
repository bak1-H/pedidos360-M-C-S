package com.pedidos360.usuarios.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Cuerpo JSON uniforme para todos los errores. La rubrica pide "codigos de error
 * adecuados": nunca devolvemos HTML generico ni un 500 disfrazado.
 */
public record ErrorResponse(
        int status,
        String codigo,
        String mensaje,
        Map<String, String> detalles,
        Instant timestamp) {

    public static ErrorResponse de(int status, String codigo, String mensaje) {
        return new ErrorResponse(status, codigo, mensaje, null, Instant.now());
    }

    public static ErrorResponse de(int status, String codigo, String mensaje, Map<String, String> detalles) {
        return new ErrorResponse(status, codigo, mensaje, detalles, Instant.now());
    }
}
