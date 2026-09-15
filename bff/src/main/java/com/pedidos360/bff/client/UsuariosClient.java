package com.pedidos360.bff.client;

import com.pedidos360.bff.dto.ActualizarRolRequest;
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
            // Primer ingreso: el perfil todavia no existe.
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

    /** Traduce el oid de Azure AD al UUID interno que usan pedidos y envios. */
    public UUID resolverUsuarioInternoId(IdentidadInterna identidad) {
        return buscarPorOid(identidad)
                .map(UsuarioDto::id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Perfil no encontrado. Llame primero a GET /bff/me."));
    }

    public UsuarioDto actualizarRol(UUID id, String rol, IdentidadInterna identidad) {
        return usuariosWebClient.patch()
                .uri("/internal/usuarios/{id}/rol", id)
                .headers(identidad::aplicar)
                .bodyValue(new ActualizarRolRequest(rol))
                .retrieve()
                .bodyToMono(UsuarioDto.class)
                .block();
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
