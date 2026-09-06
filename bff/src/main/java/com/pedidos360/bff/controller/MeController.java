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

/**
 * GET /bff/me -- lo primero que llama Angular despues del login con MSAL.
 *
 * Si el usuario nunca habia entrado, aca se le crea el perfil en usuarios-service
 * (aprovisionamiento). Ojo: esto NO es el login. El login ya ocurrio en Azure AD;
 * cuando esta peticion llega, el token ya fue validado por SecurityConfig.
 */
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
            return aPerfil(existente.get(), identidad, false);
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

    /** Azure AD manda el nombre en "name"; si el App Registration no lo expone, usamos el email. */
    private String nombreDesde(Jwt jwt) {
        String nombre = jwt.getClaimAsString("name");
        return (nombre != null && !nombre.isBlank()) ? nombre : emailDesde(jwt);
    }

    /** Segun como este configurado el tenant, el correo llega en "preferred_username", "email" o "upn". */
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
