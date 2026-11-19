package com.pedidos360.kafkaadmin.controller;

import com.pedidos360.kafkaadmin.dto.ClusterResponse;
import com.pedidos360.kafkaadmin.service.ClusterAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/kafka-admin/cluster")
public class ClusterAdminController {

    private final ClusterAdminService clusterAdminService;

    public ClusterAdminController(ClusterAdminService clusterAdminService) {
        this.clusterAdminService = clusterAdminService;
    }

    @GetMapping
    public ResponseEntity<ClusterResponse> describir() {
        return ResponseEntity.ok(clusterAdminService.describir());
    }
}
