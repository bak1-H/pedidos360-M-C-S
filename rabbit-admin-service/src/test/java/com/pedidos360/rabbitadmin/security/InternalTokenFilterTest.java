package com.pedidos360.rabbitadmin.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class InternalTokenFilterTest {

    private static final String TOKEN = "token-de-prueba";

    private final InternalTokenFilter filter = new InternalTokenFilter(TOKEN);

    private MockHttpServletRequest solicitud(String requestUri, String servletPath, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        request.setServletPath(servletPath);
        if (token != null) {
            request.addHeader("X-Internal-Token", token);
        }
        return request;
    }

    private int ejecutar(MockHttpServletRequest request, MockFilterChain chain) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response.getStatus();
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        int status = ejecutar(solicitud("/internal/queues/foo", "/internal/queues/foo", null), chain);

        assertThat(status).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void tokenIncorrectoDevuelve401() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        int status = ejecutar(solicitud("/internal/queues/foo", "/internal/queues/foo", "otro"), chain);

        assertThat(status).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void tokenValidoPasaAlControlador() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        int status = ejecutar(solicitud("/internal/queues/foo", "/internal/queues/foo", TOKEN), chain);

        assertThat(status).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void rutaConCaracteresCodificadosSinTokenDevuelve401() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        int status = ejecutar(solicitud("/%69nternal/queues/foo", "/internal/queues/foo", null), chain);

        assertThat(status).isEqualTo(401);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void actuatorNoRequiereToken() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        int status = ejecutar(solicitud("/actuator/health", "/actuator/health", null), chain);

        assertThat(status).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }
}
