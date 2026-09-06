package com.pedidos360.pedidos.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class InternalTokenFilter implements Filter {

    private final String expectedToken;

    public InternalTokenFilter(@Value("${internal.token}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!httpRequest.getRequestURI().startsWith("/internal/")) {
            chain.doFilter(request, response);
            return;
        }

        String receivedToken = httpRequest.getHeader("X-Internal-Token");
        if (expectedToken == null || !expectedToken.equals(receivedToken)) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"X-Internal-Token invalido o ausente\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
