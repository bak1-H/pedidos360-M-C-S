package com.pedidos360.bff.exception;

import java.time.Instant;

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
