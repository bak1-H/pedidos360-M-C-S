package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.dto.PedidoRequestDTO;
import com.pedidos360.pedidos.dto.PedidoResponseDTO;
import com.pedidos360.pedidos.model.DetallePedido;
import com.pedidos360.pedidos.model.EstadoPedido;
import com.pedidos360.pedidos.model.Paquete;
import com.pedidos360.pedidos.model.Pedido;
import com.pedidos360.pedidos.repository.PedidoRepository;
import com.pedidos360.pedidos.event.PedidoCreadoEvent;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository pedidoRepository;

    private final ApplicationEventPublisher eventPublisher;

    public PedidoService(PedidoRepository pedidoRepository, ApplicationEventPublisher eventPublisher) {
        this.pedidoRepository = pedidoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PedidoResponseDTO crear(PedidoRequestDTO request) {
        Pedido pedido = new Pedido();
        pedido.setClienteId(request.clienteId());
        pedido.setDireccionOrigen(request.direccionOrigen());
        pedido.setDireccionDestino(request.direccionDestino());

        request.detalles().forEach(d -> {
            DetallePedido detalle = new DetallePedido();
            detalle.setDescripcion(d.descripcion());
            detalle.setCantidad(d.cantidad());
            detalle.setValorDeclarado(d.valorDeclarado());
            pedido.agregarDetalle(detalle);
        });

        Paquete paquete = new Paquete();
        paquete.setPesoKg(request.paquete().pesoKg());
        paquete.setAlturaCm(request.paquete().alturaCm());
        paquete.setAnchoCm(request.paquete().anchoCm());
        paquete.setLargoCm(request.paquete().largoCm());
        paquete.setTipo(request.paquete().tipo());
        pedido.asignarPaquete(paquete);

        PedidoResponseDTO creado = PedidoResponseDTO.from(pedidoRepository.save(pedido));
        eventPublisher.publishEvent(new PedidoCreadoEvent(
                creado.id(), creado.clienteId(), creado.direccionDestino(), creado.fechaCreacion()));
        return creado;
    }

    @Transactional
    public void sincronizarConEnvioAsignado(UUID pedidoId) {
        avanzar(pedidoId, EstadoPedido.DESPACHADO);
    }

    @Transactional
    public void sincronizarConEstadoDeEnvio(UUID pedidoId, String estadoEnvio) {
        estadoPedidoPara(estadoEnvio).ifPresent(destino -> avanzar(pedidoId, destino));
    }

    public static Optional<EstadoPedido> estadoPedidoPara(String estadoEnvio) {
        return switch (estadoEnvio) {
            case "EN_TRANSITO" -> Optional.of(EstadoPedido.DESPACHADO);
            case "ENTREGADO" -> Optional.of(EstadoPedido.ENTREGADO);
            default -> Optional.empty();
        };
    }

    private void avanzar(UUID pedidoId, EstadoPedido destino) {
        Optional<Pedido> encontrado = pedidoRepository.findByIdForUpdate(pedidoId);
        if (encontrado.isEmpty()) {
            log.warn("Se ignora el evento de envio: no existe el pedido {}", pedidoId);
            return;
        }
        Pedido pedido = encontrado.get();
        if (pedido.getEstado().puedeAvanzarA(destino)) {
            pedido.setEstado(destino);
            pedidoRepository.save(pedido);
        }
    }

    @Transactional(readOnly = true)
    public PedidoResponseDTO buscarPorId(UUID id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pedido no encontrado: " + id));
        return PedidoResponseDTO.from(pedido);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> buscarPorCliente(UUID clienteId) {
        return pedidoRepository.findByClienteId(clienteId).stream()
                .map(PedidoResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarTodos() {
        return pedidoRepository.findAll().stream()
                .map(PedidoResponseDTO::from)
                .toList();
    }
}
