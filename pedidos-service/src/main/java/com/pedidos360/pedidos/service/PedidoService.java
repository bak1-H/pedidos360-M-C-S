package com.pedidos360.pedidos.service;

import com.pedidos360.pedidos.dto.PedidoRequestDTO;
import com.pedidos360.pedidos.dto.PedidoResponseDTO;
import com.pedidos360.pedidos.model.DetallePedido;
import com.pedidos360.pedidos.model.Paquete;
import com.pedidos360.pedidos.model.Pedido;
import com.pedidos360.pedidos.repository.PedidoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;

    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
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

        return PedidoResponseDTO.from(pedidoRepository.save(pedido));
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
