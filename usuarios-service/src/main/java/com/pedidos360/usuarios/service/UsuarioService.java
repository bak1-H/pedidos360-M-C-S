package com.pedidos360.usuarios.service;

import com.pedidos360.usuarios.dto.CrearUsuarioRequest;
import com.pedidos360.usuarios.dto.UsuarioResponse;
import com.pedidos360.usuarios.exception.RecursoNoEncontradoException;
import com.pedidos360.usuarios.exception.UsuarioDuplicadoException;
import com.pedidos360.usuarios.model.Rol;
import com.pedidos360.usuarios.model.Usuario;
import com.pedidos360.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    /** Lo usa el BFF en /bff/me para resolver el perfil a partir del claim del JWT. */
    public UsuarioResponse buscarPorAzureAdObjectId(String azureAdObjectId) {
        return usuarioRepository.findByAzureAdObjectId(azureAdObjectId)
                .map(UsuarioResponse::desde)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un usuario con azureAdObjectId " + azureAdObjectId));
    }

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::desde)
                .toList();
    }

    /** Aprovisionamiento: crea el perfil la primera vez que el usuario entra. */
    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest request) {
        if (usuarioRepository.existsByAzureAdObjectId(request.azureAdObjectId())) {
            throw new UsuarioDuplicadoException(
                    "Ya existe un usuario con azureAdObjectId " + request.azureAdObjectId());
        }

        Usuario usuario = Usuario.builder()
                .azureAdObjectId(request.azureAdObjectId())
                .nombre(request.nombre())
                .email(request.email())
                .rol(request.rol() != null ? request.rol() : Rol.CLIENTE)
                .build();

        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizarRol(UUID id, Rol nuevoRol) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe un usuario con id " + id));

        usuario.setRol(nuevoRol);
        return UsuarioResponse.desde(usuarioRepository.save(usuario));
    }
}
