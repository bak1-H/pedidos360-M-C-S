package com.pedidos360.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    /** Adjunta X-Internal-Token en cada llamada para identificarse como el BFF. */
    @Bean
    public WebClient usuariosWebClient(
            WebClient.Builder builder,
            @Value("${servicios.usuarios-url}") String usuariosUrl,
            @Value("${servicios.internal-token}") String internalToken) {

        return builder
                .baseUrl(usuariosUrl)
                .defaultHeader("X-Internal-Token", internalToken)
                .build();
    }

    @Bean
    public WebClient pedidosWebClient(
            WebClient.Builder builder,
            @Value("${servicios.pedidos-url}") String pedidosUrl,
            @Value("${servicios.internal-token}") String internalToken) {

        return builder
                .baseUrl(pedidosUrl)
                .defaultHeader("X-Internal-Token", internalToken)
                .build();
    }

    @Bean
    public WebClient enviosWebClient(
            WebClient.Builder builder,
            @Value("${servicios.envios-url}") String enviosUrl,
            @Value("${servicios.internal-token}") String internalToken) {

        return builder
                .baseUrl(enviosUrl)
                .defaultHeader("X-Internal-Token", internalToken)
                .build();
    }
}
