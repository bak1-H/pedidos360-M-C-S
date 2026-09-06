package com.pedidos360.usuarios.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedidos360.usuarios.security.InternalTokenFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeguridadInternaConfig {

    @Bean
    public FilterRegistrationBean<InternalTokenFilter> internalTokenFilter(
            @Value("${internal.token}") String tokenEsperado,
            ObjectMapper objectMapper) {

        FilterRegistrationBean<InternalTokenFilter> registro =
                new FilterRegistrationBean<>(new InternalTokenFilter(tokenEsperado, objectMapper));

        registro.addUrlPatterns("/internal/*");
        registro.setOrder(1);
        return registro;
    }
}
