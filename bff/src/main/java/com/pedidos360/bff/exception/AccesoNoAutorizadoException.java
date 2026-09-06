package com.pedidos360.bff.exception;

/**
 * Para el chequeo de "dueño o ADMIN" que SecurityConfig no puede expresar con
 * requestMatchers (depende de comparar el usuario del token contra un dato de
 * negocio, no solo del rol). Se traduce a 403 igual que un SIN_PERMISO de Spring Security.
 */
public class AccesoNoAutorizadoException extends RuntimeException {

    public AccesoNoAutorizadoException(String mensaje) {
        super(mensaje);
    }
}
