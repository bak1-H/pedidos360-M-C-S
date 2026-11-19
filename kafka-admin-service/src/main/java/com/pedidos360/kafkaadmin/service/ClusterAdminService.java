package com.pedidos360.kafkaadmin.service;

import com.pedidos360.kafkaadmin.dto.ClusterResponse;
import com.pedidos360.kafkaadmin.dto.NodoResponse;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.Node;
import org.springframework.stereotype.Service;

import java.util.Comparator;

import static com.pedidos360.kafkaadmin.service.EsperaKafka.esperar;

@Service
public class ClusterAdminService {

    private final Admin admin;

    public ClusterAdminService(Admin admin) {
        this.admin = admin;
    }

    public ClusterResponse describir() {
        DescribeClusterResult resultado = admin.describeCluster();
        Node controlador = esperar(resultado.controller());
        return new ClusterResponse(
                esperar(resultado.clusterId()),
                controlador == null ? null : nodo(controlador),
                esperar(resultado.nodes()).stream()
                        .sorted(Comparator.comparingInt(Node::id))
                        .map(this::nodo)
                        .toList());
    }

    private NodoResponse nodo(Node nodo) {
        return new NodoResponse(nodo.id(), nodo.host(), nodo.port(), nodo.rack());
    }
}
