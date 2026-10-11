package com.pedidos360.envios.service;

import com.pedidos360.envios.dto.CambioEstadoEnvioDTO;
import com.pedidos360.envios.dto.EnvioRequestDTO;
import com.pedidos360.envios.dto.EnvioResponseDTO;
import com.pedidos360.envios.event.EnvioAsignadoEvent;
import com.pedidos360.envios.event.EnvioEstadoCambiadoEvent;
import com.pedidos360.envios.exception.EnvioDuplicadoException;
import com.pedidos360.envios.exception.TransicionInvalidaException;
import com.pedidos360.envios.model.Envio;
import com.pedidos360.envios.model.EstadoEnvio;
import com.pedidos360.envios.repository.EnvioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnvioServiceTest {

    @Mock
    private EnvioRepository envioRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private Envio envioEn(EstadoEnvio estado) {
        Envio envio = new Envio();
        envio.setId(UUID.randomUUID());
        envio.setPedidoId(UUID.randomUUID());
        envio.setEstadoEnvio(estado);
        return envio;
    }

    @Test
    void creaEnvioConEventoDeAsignacion() {
        EnvioService service = new EnvioService(envioRepository, eventPublisher);
        EnvioRequestDTO request = new EnvioRequestDTO(UUID.randomUUID(), UUID.randomUUID(), null);

        when(envioRepository.saveAndFlush(any(Envio.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EnvioResponseDTO response = service.crear(request);

        assertThat(response.estadoEnvio()).isEqualTo(EstadoEnvio.PENDIENTE);
        assertThat(response.pedidoId()).isEqualTo(request.pedidoId());
        verify(eventPublisher).publishEvent(any(EnvioAsignadoEvent.class));
    }

    @Test
    void rechazaUnEnvioDuplicadoParaElMismoPedido() {
        EnvioService service = new EnvioService(envioRepository, eventPublisher);
        EnvioRequestDTO request = new EnvioRequestDTO(UUID.randomUUID(), UUID.randomUUID(), null);
        when(envioRepository.existsByPedidoId(request.pedidoId())).thenReturn(true);

        assertThatThrownBy(() -> service.crear(request)).isInstanceOf(EnvioDuplicadoException.class);

        verify(envioRepository, never()).saveAndFlush(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void conviertePorCarreraLaViolacionDeUnicidadEnEnvioDuplicado() {
        EnvioService service = new EnvioService(envioRepository, eventPublisher);
        EnvioRequestDTO request = new EnvioRequestDTO(UUID.randomUUID(), UUID.randomUUID(), null);
        when(envioRepository.saveAndFlush(any(Envio.class))).thenThrow(
                new DataIntegrityViolationException("uk", new SQLException("duplicado", "23505")));

        assertThatThrownBy(() -> service.crear(request)).isInstanceOf(EnvioDuplicadoException.class);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void noTraduceAEnvioDuplicadoUnaViolacionQueNoEsDeUnicidad() {
        EnvioService service = new EnvioService(envioRepository, eventPublisher);
        EnvioRequestDTO request = new EnvioRequestDTO(UUID.randomUUID(), UUID.randomUUID(), null);
        when(envioRepository.saveAndFlush(any(Envio.class))).thenThrow(
                new DataIntegrityViolationException("not null", new SQLException("nulo", "23502")));

        assertThatThrownBy(() -> service.crear(request)).isInstanceOf(DataIntegrityViolationException.class);

        verifyNoInteractions(eventPublisher);
    }

    @ParameterizedTest
    @CsvSource({
            "PENDIENTE, EN_TRANSITO",
            "PENDIENTE, FALLIDO",
            "EN_TRANSITO, ENTREGADO",
            "EN_TRANSITO, FALLIDO"
    })
    void aceptaLasTransicionesValidas(EstadoEnvio actual, EstadoEnvio destino) {
        EnvioService service = new EnvioService(envioRepository, eventPublisher);
        Envio existente = envioEn(actual);
        when(envioRepository.findById(existente.getId())).thenReturn(Optional.of(existente));
        when(envioRepository.save(existente)).thenReturn(existente);

        EnvioResponseDTO response = service.cambiarEstado(existente.getId(), new CambioEstadoEnvioDTO(destino, "cambio"));

        assertThat(response.estadoEnvio()).isEqualTo(destino);
        assertThat(existente.getEventos()).hasSize(1);
        verify(eventPublisher).publishEvent(any(EnvioEstadoCambiadoEvent.class));
    }

    @ParameterizedTest
    @CsvSource({
            "PENDIENTE, PENDIENTE",
            "PENDIENTE, ENTREGADO",
            "EN_TRANSITO, PENDIENTE",
            "EN_TRANSITO, EN_TRANSITO",
            "ENTREGADO, PENDIENTE",
            "ENTREGADO, EN_TRANSITO",
            "ENTREGADO, ENTREGADO",
            "ENTREGADO, FALLIDO",
            "FALLIDO, PENDIENTE",
            "FALLIDO, EN_TRANSITO",
            "FALLIDO, ENTREGADO",
            "FALLIDO, FALLIDO"
    })
    void rechazaLasTransicionesInvalidasSinPublicarEventos(EstadoEnvio actual, EstadoEnvio destino) {
        EnvioService service = new EnvioService(envioRepository, eventPublisher);
        Envio existente = envioEn(actual);
        when(envioRepository.findById(existente.getId())).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.cambiarEstado(existente.getId(), new CambioEstadoEnvioDTO(destino, "cambio")))
                .isInstanceOf(TransicionInvalidaException.class)
                .hasMessageContaining(actual.name())
                .hasMessageContaining(destino.name());

        assertThat(existente.getEstadoEnvio()).isEqualTo(actual);
        assertThat(existente.getEventos()).isEmpty();
        verify(envioRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void buscaPorIdCorrectamente() {
        EnvioService service = new EnvioService(envioRepository, eventPublisher);
        Envio existente = envioEn(EstadoEnvio.PENDIENTE);
        when(envioRepository.findById(existente.getId())).thenReturn(Optional.of(existente));

        EnvioResponseDTO response = service.buscarPorId(existente.getId());

        assertThat(response.id()).isEqualTo(existente.getId());
    }

    @Test
    void lanzaExcepcionSiEnvioNoExiste() {
        EnvioService service = new EnvioService(envioRepository, eventPublisher);
        UUID id = UUID.randomUUID();
        when(envioRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cambiarEstado(id, new CambioEstadoEnvioDTO(EstadoEnvio.FALLIDO, "No encontrado")))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void listaTodosLosEnviosEnElOrdenDelRepositorio() {
        EnvioService service = new EnvioService(envioRepository, eventPublisher);
        Envio reciente = envioEn(EstadoEnvio.PENDIENTE);
        reciente.setFechaAsignacion(LocalDateTime.now());
        Envio antiguo = envioEn(EstadoEnvio.ENTREGADO);
        antiguo.setFechaAsignacion(LocalDateTime.now().minusDays(1));
        when(envioRepository.findAllByOrderByFechaAsignacionDesc()).thenReturn(List.of(reciente, antiguo));

        List<EnvioResponseDTO> envios = service.listarTodos();

        assertThat(envios).extracting(EnvioResponseDTO::id).containsExactly(reciente.getId(), antiguo.getId());
    }
}
