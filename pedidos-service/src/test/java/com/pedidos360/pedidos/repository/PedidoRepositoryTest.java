package com.pedidos360.pedidos.repository;

import com.pedidos360.pedidos.model.DetallePedido;
import com.pedidos360.pedidos.model.Paquete;
import com.pedidos360.pedidos.model.Pedido;
import com.pedidos360.pedidos.model.TipoPaquete;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PedidoRepositoryTest {

    @Autowired
    private PedidoRepository pedidoRepository;

    private Pedido construirPedido(UUID clienteId) {
        Pedido pedido = new Pedido();
        pedido.setClienteId(clienteId);
        pedido.setDireccionOrigen("Av. Siempre Viva 123");
        pedido.setDireccionDestino("Calle Falsa 456");

        DetallePedido detalle = new DetallePedido();
        detalle.setDescripcion("Notebook");
        detalle.setCantidad(1);
        detalle.setValorDeclarado(BigDecimal.valueOf(500000));
        pedido.agregarDetalle(detalle);

        Paquete paquete = new Paquete();
        paquete.setPesoKg(BigDecimal.valueOf(2.5));
        paquete.setAlturaCm(BigDecimal.TEN);
        paquete.setAnchoCm(BigDecimal.TEN);
        paquete.setLargoCm(BigDecimal.TEN);
        paquete.setTipo(TipoPaquete.CAJA);
        pedido.asignarPaquete(paquete);

        return pedido;
    }

    @Test
    void guardaPedidoConDetalleYPaqueteYLosRecupera() {
        Pedido guardado = pedidoRepository.saveAndFlush(construirPedido(UUID.randomUUID()));
        pedidoRepository.flush();

        Optional<Pedido> recuperado = pedidoRepository.findById(guardado.getId());

        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getDetalles()).hasSize(1);
        assertThat(recuperado.get().getPaquete()).isNotNull();
        assertThat(recuperado.get().getPaquete().getTipo()).isEqualTo(TipoPaquete.CAJA);
    }

    @Test
    void encuentraPedidosPorClienteId() {
        UUID clienteId = UUID.randomUUID();
        pedidoRepository.saveAndFlush(construirPedido(clienteId));
        pedidoRepository.saveAndFlush(construirPedido(UUID.randomUUID()));

        List<Pedido> pedidosDelCliente = pedidoRepository.findByClienteId(clienteId);

        assertThat(pedidosDelCliente).hasSize(1);
        assertThat(pedidosDelCliente.get(0).getClienteId()).isEqualTo(clienteId);
    }
}
