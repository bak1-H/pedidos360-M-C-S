package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.dto.DetallePedidoDTO;
import com.pedidos360.pedidos.dto.PaqueteDTO;
import com.pedidos360.pedidos.dto.PedidoRequestDTO;
import com.pedidos360.pedidos.dto.PedidoResponseDTO;
import com.pedidos360.pedidos.model.Pedido;
import com.pedidos360.pedidos.model.TipoPaquete;
import com.pedidos360.pedidos.repository.PedidoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Test
    void creaPedidoConDetalleYPaquete() {
        PedidoService service = new PedidoService(pedidoRepository);
        PedidoRequestDTO request = new PedidoRequestDTO(
                UUID.randomUUID(),
                "Av. Siempre Viva 123",
                "Calle Falsa 456",
                List.of(new DetallePedidoDTO("Notebook", 1, BigDecimal.valueOf(500000))),
                new PaqueteDTO(BigDecimal.valueOf(2.5), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, TipoPaquete.CAJA)
        );

        ArgumentCaptor<Pedido> captor = ArgumentCaptor.forClass(Pedido.class);
        when(pedidoRepository.save(captor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        PedidoResponseDTO response = service.crear(request);

        Pedido guardado = captor.getValue();
        assertThat(guardado.getDetalles()).hasSize(1);
        assertThat(guardado.getPaquete()).isNotNull();
        assertThat(guardado.getPaquete().getPedido()).isSameAs(guardado);
        assertThat(response.direccionOrigen()).isEqualTo("Av. Siempre Viva 123");
    }

    @Test
    void lanzaExcepcionSiPedidoNoExiste() {
        PedidoService service = new PedidoService(pedidoRepository);
        UUID id = UUID.randomUUID();
        when(pedidoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
