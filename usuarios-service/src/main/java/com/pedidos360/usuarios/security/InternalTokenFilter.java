package com.pedidos360.usuarios.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.usuarios.exception.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Los endpoints /internal/** no son publicos. La proteccion principal es de red
 * (solo el BFF los alcanza), pero el SDD pide "como minimo un header interno simple"
 * para que no queden completamente abiertos si la regla de red falla.
 *
 * No reemplaza a la validacion JWT: esa vive en el BFF. Aca solo comprobamos que
 * quien llama sea nuestro propio BFF.
 */
@RequiredArgsConstructor
public class InternalTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Token";

    private final String tokenEsperado;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String recibido = request.getHeader(HEADER);

        if (!coincide(recibido, tokenEsperado)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(
                    response.getWriter(),
                    ErrorResponse.de(401, "TOKEN_INTERNO_INVALIDO",
                            "Falta o es incorrecto el header " + HEADER));
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** Comparacion en tiempo constante, para no filtrar el token por timing. */
    private boolean coincide(String recibido, String esperado) {
        if (recibido == null || esperado == null) {
            return false;
        }
        return MessageDigest.isEqual(
                recibido.getBytes(StandardCharsets.UTF_8),
                esperado.getBytes(StandardCharsets.UTF_8));
    }
}
