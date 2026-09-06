package com.pedidos360.bff.client;

import com.pedidos360.bff.dto.CrearUsuarioRequest;
import com.pedidos360.bff.dto.UsuarioDto;
import com.pedidos360.bff.exception.RecursoNoEncontradoException;
import com.pedidos360.bff.security.IdentidadInterna;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsuariosClient {

    private final WebClient usuariosWebClient;

    public Optional<UsuarioDto> buscarPorOid(IdentidadInterna identidad) {
        try {
            UsuarioDto usuario = usuariosWebClient.get()
                    .uri("/internal/usuarios/{oid}", identidad.oid())
                    .headers(identidad::aplicar)
                    .retrieve()
                    .bodyToMono(UsuarioDto.class)
                    .block();
            return Optional.ofNullable(usuario);

        } catch (WebClientResponseException.NotFound e) {
            // El perfil todavia no existe: es la primera vez que este usuario entra.
            return Optional.empty();
        }
    }

    public UsuarioDto crear(CrearUsuarioRequest request, IdentidadInterna identidad) {
        return usuariosWebClient.post()
                .uri("/internal/usuarios")
                .headers(identidad::aplicar)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UsuarioDto.class)
                .block();
    }

    /**
     * pedidos-service y envios-service guardan clienteId/repartidorId como el UUID
     * interno de usuarios-service, no como el oid de Azure AD. Todo controller que
     * necesite ese UUID para armar una peticion hacia esos dos servicios pasa por aca.
     */
    public UUID resolverUsuarioInternoId(IdentidadInterna identidad) {
        return buscarPorOid(identidad)
                .map(UsuarioDto::id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Perfil no encontrado. Llame primero a GET /bff/me."));
    }

    public List<UsuarioDto> listar(IdentidadInterna identidad) {
        return usuariosWebClient.get()
                .uri("/internal/usuarios")
                .headers(identidad::aplicar)
                .retrieve()
                .bodyToFlux(UsuarioDto.class)
                .collectList()
                .block();
    }
}
