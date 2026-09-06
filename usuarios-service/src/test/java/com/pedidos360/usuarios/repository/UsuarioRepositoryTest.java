package com.pedidos360.usuarios.repository;

import com.pedidos360.usuarios.model.Rol;
import com.pedidos360.usuarios.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Usuario nuevoUsuario(String oid) {
        return Usuario.builder()
                .azureAdObjectId(oid)
                .nombre("Sebastian Riveros")
                .email("sebastian@pedidos360.cl")
                .rol(Rol.CLIENTE)
                .build();
    }

    @Test
    @DisplayName("guarda un usuario y lo recupera por su azureAdObjectId")
    void guardaYRecuperaPorAzureAdObjectId() {
        usuarioRepository.save(nuevoUsuario("oid-abc-123"));
        entityManager.flush();
        entityManager.clear();

        Optional<Usuario> encontrado = usuarioRepository.findByAzureAdObjectId("oid-abc-123");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getEmail()).isEqualTo("sebastian@pedidos360.cl");
        assertThat(encontrado.get().getRol()).isEqualTo(Rol.CLIENTE);
    }

    @Test
    @DisplayName("genera el UUID y la fechaCreacion al persistir")
    void asignaIdYFechaCreacion() {
        Usuario guardado = usuarioRepository.save(nuevoUsuario("oid-def-456"));
        entityManager.flush();

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getFechaCreacion()).isNotNull();
    }

    @Test
    @DisplayName("existsByAzureAdObjectId distingue un oid registrado de uno que no")
    void existsByAzureAdObjectId() {
        usuarioRepository.save(nuevoUsuario("oid-ghi-789"));
        entityManager.flush();

        assertThat(usuarioRepository.existsByAzureAdObjectId("oid-ghi-789")).isTrue();
        assertThat(usuarioRepository.existsByAzureAdObjectId("oid-que-no-existe")).isFalse();
    }
}
