package com.pedidos360.bff.exception;

import java.time.Instant;

/**
 * Cuerpo JSON uniforme de error. La rubrica exige "codigos de error adecuados":
 * 401 sin token o con token invalido, 403 sin el rol necesario, y siempre JSON,
 * nunca la pagina HTML de error por defecto de Spring.
 */
public record ErrorResponse(
        int status,
        String codigo,
        String mensaje,
        String ruta,
        Instant timestamp) {

    public static ErrorResponse de(int status, String codigo, String mensaje, String ruta) {
        return new ErrorResponse(status, codigo, mensaje, ruta, Instant.now());
    }
}
