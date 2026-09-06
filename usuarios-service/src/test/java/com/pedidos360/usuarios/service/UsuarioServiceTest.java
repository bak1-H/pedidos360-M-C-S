package com.pedidos360.usuarios.service;

import com.pedidos360.usuarios.dto.CrearUsuarioRequest;
import com.pedidos360.usuarios.dto.UsuarioResponse;
import com.pedidos360.usuarios.exception.RecursoNoEncontradoException;
import com.pedidos360.usuarios.exception.UsuarioDuplicadoException;
import com.pedidos360.usuarios.model.Rol;
import com.pedidos360.usuarios.model.Usuario;
import com.pedidos360.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    @DisplayName("crear() deja el rol en CLIENTE cuando el request no trae rol")
    void crearAsignaClientePorDefecto() {
        var request = new CrearUsuarioRequest("oid-1", "Ana Soto", "ana@pedidos360.cl", null);
        when(usuarioRepository.existsByAzureAdObjectId("oid-1")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse creado = usuarioService.crear(request);

        assertThat(creado.rol()).isEqualTo(Rol.CLIENTE);
        assertThat(creado.email()).isEqualTo("ana@pedidos360.cl");
    }

    @Test
    @DisplayName("crear() respeta el rol explicito que manda el BFF")
    void crearRespetaRolExplicito() {
        var request = new CrearUsuarioRequest("oid-2", "Luis Paz", "luis@pedidos360.cl", Rol.REPARTIDOR);
        when(usuarioRepository.existsByAzureAdObjectId("oid-2")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(usuarioService.crear(request).rol()).isEqualTo(Rol.REPARTIDOR);
    }

    @Test
    @DisplayName("crear() lanza UsuarioDuplicadoException si el oid ya existe y no guarda nada")
    void crearRechazaDuplicado() {
        var request = new CrearUsuarioRequest("oid-3", "Ana Soto", "ana@pedidos360.cl", null);
        when(usuarioRepository.existsByAzureAdObjectId("oid-3")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(request))
                .isInstanceOf(UsuarioDuplicadoException.class)
                .hasMessageContaining("oid-3");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("buscarPorAzureAdObjectId() lanza 404 logico si el perfil no existe")
    void buscarLanzaNoEncontrado() {
        when(usuarioRepository.findByAzureAdObjectId("oid-fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.buscarPorAzureAdObjectId("oid-fantasma"))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("actualizarRol() persiste el nuevo rol")
    void actualizarRolPersiste() {
        UUID id = UUID.randomUUID();
        Usuario existente = Usuario.builder()
                .id(id)
                .azureAdObjectId("oid-4")
                .nombre("Ana Soto")
                .email("ana@pedidos360.cl")
                .rol(Rol.CLIENTE)
                .build();

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(usuarioService.actualizarRol(id, Rol.ADMIN).rol()).isEqualTo(Rol.ADMIN);
    }
}
