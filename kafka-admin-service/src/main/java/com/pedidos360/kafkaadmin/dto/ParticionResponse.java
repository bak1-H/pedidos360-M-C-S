package com.pedidos360.kafkaadmin.dto;

import java.util.List;

public record ParticionResponse(int id, Integer lider, List<Integer> replicas, List<Integer> isr) {
}
