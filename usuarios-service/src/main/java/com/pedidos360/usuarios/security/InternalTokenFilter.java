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

/** Comprueba que quien llama a /internal/** sea el BFF; la validacion del JWT vive alla. */
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

    /** Comparacion en tiempo constante para no filtrar el token por timing. */
    private boolean coincide(String recibido, String esperado) {
        if (recibido == null || esperado == null) {
            return false;
        }
        return MessageDigest.isEqual(
                recibido.getBytes(StandardCharsets.UTF_8),
                esperado.getBytes(StandardCharsets.UTF_8));
    }
}
