package com.pedidos360.bff.controller;

import com.pedidos360.bff.client.UsuariosClient;
import com.pedidos360.bff.dto.UsuarioDto;
import com.pedidos360.bff.security.IdentidadInterna;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/bff")
@RequiredArgsConstructor
public class UsuariosBffController {

    private final UsuariosClient usuariosClient;

    @GetMapping("/usuarios")
    public List<UsuarioDto> listar(@AuthenticationPrincipal Jwt jwt) {
        return usuariosClient.listar(IdentidadInterna.desde(jwt));
    }
}
