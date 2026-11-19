package com.pedidos360.kafkaadmin.service;

import com.pedidos360.kafkaadmin.dto.GrupoDetalleResponse;
import com.pedidos360.kafkaadmin.dto.GrupoResumenResponse;
import com.pedidos360.kafkaadmin.dto.MiembroResponse;
import com.pedidos360.kafkaadmin.dto.OffsetParticionResponse;
import com.pedidos360.kafkaadmin.dto.PatronesKafka;
import com.pedidos360.kafkaadmin.dto.ResetOffsetsRequest;
import com.pedidos360.kafkaadmin.exception.ConflictoException;
import com.pedidos360.kafkaadmin.exception.RecursoNoEncontradoException;
import com.pedidos360.kafkaadmin.exception.SolicitudInvalidaException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.pedidos360.kafkaadmin.service.EsperaKafka.esperar;

@Service
public class GrupoAdminService {

    private static final String DESTINO_EARLIEST = "earliest";

    private final Admin admin;

    public GrupoAdminService(Admin admin) {
        this.admin = admin;
    }

    public List<GrupoResumenResponse> listar() {
        return esperar(admin.listConsumerGroups().all()).stream()
                .map(grupo -> new GrupoResumenResponse(grupo.groupId(),
                        grupo.state().map(ConsumerGroupState::name).orElse("UNKNOWN")))
                .sorted(Comparator.comparing(GrupoResumenResponse::id))
                .toList();
    }

    public GrupoDetalleResponse obtener(String id) {
        ConsumerGroupDescription descripcion = describir(id);
        Map<TopicPartition, OffsetAndMetadata> confirmados = offsetsConfirmados(id);
        List<OffsetParticionResponse> offsets = calcularOffsets(confirmados, offsets(confirmados.keySet(), OffsetSpec.latest()));
        return new GrupoDetalleResponse(
                id,
                descripcion.state().name(),
                descripcion.members().stream().map(this::miembro).toList(),
                offsets,
                offsets.stream().mapToLong(OffsetParticionResponse::lag).sum());
    }

    public List<OffsetParticionResponse> resetearOffsets(String id, ResetOffsetsRequest request) {
        if (request.destino() == null || !request.destino().matches(PatronesKafka.DESTINO_OFFSETS)) {
            throw new SolicitudInvalidaException("destino debe ser earliest o latest");
        }
        ConsumerGroupDescription descripcion = describir(id);
        if (descripcion.state() != ConsumerGroupState.EMPTY) {
            throw new ConflictoException(
                    "El grupo debe estar EMPTY para resetear offsets (estado actual: " + descripcion.state().name() + ")");
        }
        Set<TopicPartition> particiones = offsetsConfirmados(id).keySet().stream()
                .filter(tp -> request.topico() == null || request.topico().isBlank() || tp.topic().equals(request.topico()))
                .collect(Collectors.toSet());
        if (particiones.isEmpty()) {
            throw new SolicitudInvalidaException("El grupo no tiene offsets confirmados para resetear");
        }

        Map<TopicPartition, Long> finales = offsets(particiones, OffsetSpec.latest());
        Map<TopicPartition, Long> nuevos = DESTINO_EARLIEST.equals(request.destino())
                ? offsets(particiones, OffsetSpec.earliest())
                : finales;
        Map<TopicPartition, OffsetAndMetadata> nuevosConfirmados = new HashMap<>();
        nuevos.forEach((tp, offset) -> nuevosConfirmados.put(tp, new OffsetAndMetadata(offset)));
        esperar(admin.alterConsumerGroupOffsets(id, nuevosConfirmados).all());

        return calcularOffsets(nuevosConfirmados, finales);
    }

    static List<OffsetParticionResponse> calcularOffsets(Map<TopicPartition, OffsetAndMetadata> confirmados,
                                                         Map<TopicPartition, Long> finales) {
        return confirmados.entrySet().stream()
                .map(entrada -> {
                    long confirmado = entrada.getValue().offset();
                    long fin = finales.getOrDefault(entrada.getKey(), confirmado);
                    return new OffsetParticionResponse(entrada.getKey().topic(), entrada.getKey().partition(),
                            confirmado, fin, Math.max(0, fin - confirmado));
                })
                .sorted(Comparator.comparing(OffsetParticionResponse::topico)
                        .thenComparingInt(OffsetParticionResponse::particion))
                .toList();
    }

    private ConsumerGroupDescription describir(String id) {
        if (id == null || id.isBlank()) {
            throw new SolicitudInvalidaException("id del grupo es obligatorio");
        }
        ConsumerGroupDescription descripcion = esperar(admin.describeConsumerGroups(List.of(id)).describedGroups().get(id));
        if (descripcion.state() == ConsumerGroupState.DEAD) {
            throw new RecursoNoEncontradoException("El grupo no existe");
        }
        return descripcion;
    }

    private Map<TopicPartition, OffsetAndMetadata> offsetsConfirmados(String id) {
        Map<TopicPartition, OffsetAndMetadata> confirmados =
                esperar(admin.listConsumerGroupOffsets(id).partitionsToOffsetAndMetadata());
        Map<TopicPartition, OffsetAndMetadata> validos = new HashMap<>();
        confirmados.forEach((tp, offset) -> {
            if (offset != null) {
                validos.put(tp, offset);
            }
        });
        return validos;
    }

    private Map<TopicPartition, Long> offsets(Set<TopicPartition> particiones, OffsetSpec especificacion) {
        if (particiones.isEmpty()) {
            return Map.of();
        }
        Map<TopicPartition, OffsetSpec> consulta = new HashMap<>();
        particiones.forEach(tp -> consulta.put(tp, especificacion));
        Map<TopicPartition, ListOffsetsResultInfo> resultado = esperar(admin.listOffsets(consulta).all());
        Map<TopicPartition, Long> offsets = new HashMap<>();
        resultado.forEach((tp, info) -> offsets.put(tp, info.offset()));
        return offsets;
    }

    private MiembroResponse miembro(MemberDescription miembro) {
        return new MiembroResponse(miembro.consumerId(), miembro.clientId(), miembro.host(),
                miembro.assignment().topicPartitions().stream()
                        .sorted(Comparator.comparing(TopicPartition::topic).thenComparingInt(TopicPartition::partition))
                        .map(tp -> tp.topic() + "-" + tp.partition())
                        .toList());
    }
}
