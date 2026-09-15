package com.pedidos360.bff.controller;

import com.pedidos360.bff.client.UsuariosClient;
import com.pedidos360.bff.dto.CrearUsuarioRequest;
import com.pedidos360.bff.dto.PerfilResponse;
import com.pedidos360.bff.dto.UsuarioDto;
import com.pedidos360.bff.security.IdentidadInterna;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/** Devuelve el perfil y lo crea si es el primer ingreso del usuario. */
@Slf4j
@RestController
@RequestMapping("/bff")
@RequiredArgsConstructor
public class MeController {

    private final UsuariosClient usuariosClient;

    @GetMapping("/me")
    public PerfilResponse me(@AuthenticationPrincipal Jwt jwt) {
        IdentidadInterna identidad = IdentidadInterna.desde(jwt);

        Optional<UsuarioDto> existente = usuariosClient.buscarPorOid(identidad);
        if (existente.isPresent()) {
            return aPerfil(sincronizarRol(existente.get(), identidad), identidad, false);
        }

        log.info("Aprovisionando perfil nuevo para oid {}", identidad.oid());
        UsuarioDto creado = usuariosClient.crear(
                new CrearUsuarioRequest(
                        identidad.oid(),
                        nombreDesde(jwt),
                        emailDesde(jwt),
                        identidad.rolPrincipal()),
                identidad);

        return aPerfil(creado, identidad, true);
    }

    /** Realinea el rol guardado con el del token; si el token no trae roles no toca nada. */
    private UsuarioDto sincronizarRol(UsuarioDto usuario, IdentidadInterna identidad) {
        if (identidad.roles().isEmpty()) {
            return usuario;
        }

        String rolDelToken = identidad.rolPrincipal();
        if (rolDelToken.equals(usuario.rol())) {
            return usuario;
        }

        log.info("Rol de {} desactualizado: {} en base, {} en Azure AD. Sincronizando.",
                identidad.oid(), usuario.rol(), rolDelToken);

        return usuariosClient.actualizarRol(usuario.id(), rolDelToken, identidad);
    }

    private PerfilResponse aPerfil(UsuarioDto usuario, IdentidadInterna identidad, boolean recienCreado) {
        return new PerfilResponse(
                usuario.id(),
                usuario.azureAdObjectId(),
                usuario.nombre(),
                usuario.email(),
                usuario.rol(),
                identidad.roles(),
                recienCreado);
    }

    /** Toma el claim "name" y cae al email si no viene. */
    private String nombreDesde(Jwt jwt) {
        String nombre = jwt.getClaimAsString("name");
        return (nombre != null && !nombre.isBlank()) ? nombre : emailDesde(jwt);
    }

    /** El correo llega en "preferred_username", "email" o "upn" segun el tenant. */
    private String emailDesde(Jwt jwt) {
        for (String claim : new String[]{"preferred_username", "email", "upn"}) {
            String valor = jwt.getClaimAsString(claim);
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return jwt.getSubject() + "@sin-email.local";
    }
}
