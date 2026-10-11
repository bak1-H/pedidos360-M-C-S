package com.pedidos360.envios.service;

import com.pedidos360.envios.dto.CambioEstadoEnvioDTO;
import com.pedidos360.envios.dto.EnvioRequestDTO;
import com.pedidos360.envios.dto.EnvioResponseDTO;
import com.pedidos360.envios.event.EnvioAsignadoEvent;
import com.pedidos360.envios.event.EnvioEntregadoEvent;
import com.pedidos360.envios.event.EnvioEstadoCambiadoEvent;
import com.pedidos360.envios.model.Envio;
import com.pedidos360.envios.model.EstadoEnvio;
import com.pedidos360.envios.repository.EnvioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnvioServiceTest {

    @Mock
    private EnvioRepository envioRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void creaEnvioConEventoDeAsignacion() {
        EnvioService service = new EnvioService(envioRepository, eventPublisher);
        EnvioRequestDTO request = new EnvioRequestDTO(UUID.randomUUID(), UUID.randomUUID(), null);

        when(envioRepository.save(org.mockito.ArgumentMatchers.any(Envio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EnvioResponseDTO response = service.crear(request);

        assertThat(response.estadoEnvio()).isEqualTo(EstadoEnvio.PENDIENTE);
        assertThat(response.pedidoId()).isEqualTo(request.pedidoId());
        verify(eventPublisher).publishEvent(any(EnvioAsignadoEvent.class));
    }

    @Test
    void cambiaEstadoYRegistraEvento() {
        EnvioService service = new EnvioService(envioRepository, eventPublisher);
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
        verify(eventPublisher).publishEvent(any(EnvioEstadoCambiadoEvent.class));
        verify(eventPublisher, never()).publishEvent(any(EnvioEntregadoEvent.class));
    }

    @Test
    void publicaEventoDeEntregaSoloCuandoElEstadoEsEntregado() {
        EnvioService service = new EnvioService(envioRepository, eventPublisher);
        UUID id = UUID.randomUUID();
        Envio existente = new Envio();
        existente.setId(id);
        existente.setPedidoId(UUID.randomUUID());

        when(envioRepository.findById(id)).thenReturn(Optional.of(existente));
        when(envioRepository.save(existente)).thenReturn(existente);

        service.cambiarEstado(id, new CambioEstadoEnvioDTO(EstadoEnvio.ENTREGADO, "Entregado al cliente"));

        verify(eventPublisher).publishEvent(any(EnvioEstadoCambiadoEvent.class));
        verify(eventPublisher).publishEvent(any(EnvioEntregadoEvent.class));
    }

    @Test
    void buscaPorIdCorrectamente() {
        EnvioService service = new EnvioService(envioRepository, eventPublisher);
        UUID id = UUID.randomUUID();
        Envio existente = new Envio();
        existente.setId(id);
        existente.setPedidoId(UUID.randomUUID());

        when(envioRepository.findById(id)).thenReturn(Optional.of(existente));

        EnvioResponseDTO response = service.buscarPorId(id);

        assertThat(response.id()).isEqualTo(id);
    }

    @Test
    void lanzaExcepcionSiEnvioNoExiste() {
        EnvioService service = new EnvioService(envioRepository, eventPublisher);
        UUID id = UUID.randomUUID();
        when(envioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstado(id, new CambioEstadoEnvioDTO(EstadoEnvio.FALLIDO, "No encontrado")))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
