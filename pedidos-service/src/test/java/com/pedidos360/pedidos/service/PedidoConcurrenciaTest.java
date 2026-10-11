package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.model.EstadoPedido;
import com.pedidos360.pedidos.model.Pedido;
import com.pedidos360.pedidos.repository.PedidoRepository;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(PedidoService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PedidoConcurrenciaTest {

    private static final int ITERACIONES = 100;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Test
    void laConsultaDeActualizacionUsaBloqueoPesimistaDeEscritura() throws NoSuchMethodException {
        Lock lock = PedidoRepository.class.getMethod("findByIdForUpdate", UUID.class).getAnnotation(Lock.class);

        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRED)
    void laConsultaDeActualizacionFuncionaYDevuelveVacioSiNoExiste() {
        Pedido guardado = pedidoRepository.saveAndFlush(nuevoPedido());

        assertThat(pedidoRepository.findByIdForUpdate(guardado.getId())).isPresent();
        assertThat(pedidoRepository.findByIdForUpdate(UUID.randomUUID())).isEmpty();
    }

    @Test
    void dosEventosConcurrentesTerminanSiempreEnEntregado() throws Exception {
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < ITERACIONES; i++) {
                UUID pedidoId = pedidoRepository.saveAndFlush(nuevoPedido()).getId();
                boolean entregadoPrimero = i % 2 == 0;
                CountDownLatch salida = new CountDownLatch(1);

                Future<?> asignado = ejecutor.submit(() -> {
                    esperar(salida);
                    pedidoService.sincronizarConEnvioAsignado(pedidoId);
                });
                Future<?> entregado = ejecutor.submit(() -> {
                    esperar(salida);
                    pedidoService.sincronizarConEstadoDeEnvio(pedidoId, "ENTREGADO");
                });
                salida.countDown();
                (entregadoPrimero ? entregado : asignado).get(10, TimeUnit.SECONDS);
                (entregadoPrimero ? asignado : entregado).get(10, TimeUnit.SECONDS);

                EstadoPedido finalEstado = pedidoRepository.findById(pedidoId).orElseThrow().getEstado();
                assertThat(finalEstado).as("iteracion %d", i).isEqualTo(EstadoPedido.ENTREGADO);
            }
        } finally {
            ejecutor.shutdownNow();
        }
    }

    private void esperar(CountDownLatch salida) {
        try {
            salida.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private Pedido nuevoPedido() {
        Pedido pedido = new Pedido();
        pedido.setClienteId(UUID.randomUUID());
        pedido.setDireccionOrigen("Av. Siempre Viva 123");
        pedido.setDireccionDestino("Calle Falsa 456");
        return pedido;
    }
}
