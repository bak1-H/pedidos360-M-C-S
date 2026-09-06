package com.pedidos360.envios.repository;

import com.pedidos360.envios.model.Envio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnvioRepository extends JpaRepository<Envio, UUID> {

    List<Envio> findByRepartidorId(UUID repartidorId);

    Optional<Envio> findByPedidoId(UUID pedidoId);
}
