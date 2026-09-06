package com.pedidos360.envios.service;

import com.pedidos360.envios.dto.CambioEstadoEnvioDTO;
import com.pedidos360.envios.dto.EnvioRequestDTO;
import com.pedidos360.envios.dto.EnvioResponseDTO;
import com.pedidos360.envios.model.Envio;
import com.pedidos360.envios.model.EstadoEnvio;
import com.pedidos360.envios.repository.EnvioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnvioServiceTest {

    @Mock
    private EnvioRepository envioRepository;

    @Test
    void creaEnvioConEventoDeAsignacion() {
        EnvioService service = new EnvioService(envioRepository);
        EnvioRequestDTO request = new EnvioRequestDTO(UUID.randomUUID(), UUID.randomUUID(), null);

        when(envioRepository.save(org.mockito.ArgumentMatchers.any(Envio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EnvioResponseDTO response = service.crear(request);

        assertThat(response.estadoEnvio()).isEqualTo(EstadoEnvio.PENDIENTE);
        assertThat(response.pedidoId()).isEqualTo(request.pedidoId());
    }

    @Test
    void cambiaEstadoYRegistraEvento() {
        EnvioService service = new EnvioService(envioRepository);
        UUID id = UUID.randomUUID();
        Envio existente = new Envio();
        existente.setId(id);
        existente.setPedidoId(UUID.randomUUID());
        existente.setEstadoEnvio(EstadoEnvio.PENDIENTE);

        when(envioRepository.findById(id)).thenReturn(Optional.of(existente));
        when(envioRepository.save(existente)).thenReturn(existente);

        EnvioResponseDTO response = service.cambiarEstado(id, new CambioEstadoEnvioDTO(EstadoEnvio.EN_TRANSITO, "Salio a reparto"));

        assertThat(response.estadoEnvio()).isEqualTo(EstadoEnvio.EN_TRANSITO);
        assertThat(existente.getEventos()).hasSize(1);
    }

    @Test
    void lanzaExcepcionSiEnvioNoExiste() {
        EnvioService service = new EnvioService(envioRepository);
        UUID id = UUID.randomUUID();
        when(envioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstado(id, new CambioEstadoEnvioDTO(EstadoEnvio.FALLIDO, "No encontrado")))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
