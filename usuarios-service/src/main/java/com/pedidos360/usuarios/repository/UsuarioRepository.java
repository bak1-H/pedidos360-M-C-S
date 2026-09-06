package com.pedidos360.usuarios.repository;

import com.pedidos360.usuarios.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByAzureAdObjectId(String azureAdObjectId);

    boolean existsByAzureAdObjectId(String azureAdObjectId);
}
