package com.pedidos360.kafkaadmin.service;

import com.pedidos360.kafkaadmin.dto.GrupoDetalleResponse;
import com.pedidos360.kafkaadmin.dto.OffsetParticionResponse;
import com.pedidos360.kafkaadmin.dto.ResetOffsetsRequest;
import com.pedidos360.kafkaadmin.exception.ConflictoException;
import com.pedidos360.kafkaadmin.exception.RecursoNoEncontradoException;
import com.pedidos360.kafkaadmin.exception.SolicitudInvalidaException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrupoAdminServiceTest {

    private static final String GRUPO = "notificaciones-analitica";
    private static final TopicPartition P0 = new TopicPartition("pedidos360.pedidos.eventos", 0);
    private static final TopicPartition P1 = new TopicPartition("pedidos360.pedidos.eventos", 1);

    @Mock
    private Admin admin;

    private void grupoEnEstado(ConsumerGroupState estado) {
        ConsumerGroupDescription descripcion = mock(ConsumerGroupDescription.class);
        when(descripcion.state()).thenReturn(estado);
        DescribeConsumerGroupsResult resultado = mock(DescribeConsumerGroupsResult.class);
        when(resultado.describedGroups()).thenReturn(Map.of(GRUPO, KafkaFuture.completedFuture(descripcion)));
        when(admin.describeConsumerGroups(List.of(GRUPO))).thenReturn(resultado);
    }

    private void offsetsConfirmados(Map<TopicPartition, OffsetAndMetadata> confirmados) {
        ListConsumerGroupOffsetsResult resultado = mock(ListConsumerGroupOffsetsResult.class);
        when(resultado.partitionsToOffsetAndMetadata()).thenReturn(KafkaFuture.completedFuture(confirmados));
        when(admin.listConsumerGroupOffsets(GRUPO)).thenReturn(resultado);
    }

    private void offsetsDelBroker(Map<TopicPartition, Long> finales, Map<TopicPartition, Long> iniciales) {
        when(admin.listOffsets(anyMap())).thenAnswer(invocacion -> {
            Map<TopicPartition, OffsetSpec> consulta = invocacion.getArgument(0);
            Map<TopicPartition, ListOffsetsResultInfo> info = new HashMap<>();
            consulta.forEach((tp, spec) -> {
                long offset = spec.getClass().getSimpleName().startsWith("Earliest") ? iniciales.get(tp) : finales.get(tp);
                info.put(tp, new ListOffsetsResultInfo(offset, 0L, Optional.empty()));
            });
            ListOffsetsResult resultado = mock(ListOffsetsResult.class);
            when(resultado.all()).thenReturn(KafkaFuture.completedFuture(info));
            return resultado;
        });
    }

    @Test
    void calculaElLagPorParticionYElTotal() {
        grupoEnEstado(ConsumerGroupState.STABLE);
        offsetsConfirmados(Map.of(P0, new OffsetAndMetadata(10), P1, new OffsetAndMetadata(7)));
        offsetsDelBroker(Map.of(P0, 25L, P1, 7L), Map.of());

        GrupoDetalleResponse detalle = new GrupoAdminService(admin).obtener(GRUPO);

        assertThat(detalle.estado()).isEqualTo("STABLE");
        assertThat(detalle.offsets()).extracting(OffsetParticionResponse::lag).containsExactly(15L, 0L);
        assertThat(detalle.lagTotal()).isEqualTo(15L);
    }

    @Test
    void elLagNuncaEsNegativo() {
        List<OffsetParticionResponse> offsets = GrupoAdminService.calcularOffsets(
                Map.of(P0, new OffsetAndMetadata(30)), Map.of(P0, 20L));

        assertThat(offsets.get(0).lag()).isZero();
    }

    @Test
    void unGrupoInexistenteDevuelveNoEncontrado() {
        grupoEnEstado(ConsumerGroupState.DEAD);

        assertThatThrownBy(() -> new GrupoAdminService(admin).obtener(GRUPO))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void noReseteaOffsetsSiElGrupoNoEstaVacio() {
        grupoEnEstado(ConsumerGroupState.STABLE);

        assertThatThrownBy(() -> new GrupoAdminService(admin).resetearOffsets(GRUPO, new ResetOffsetsRequest("earliest", null)))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("EMPTY");

        verify(admin, never()).alterConsumerGroupOffsets(any(), anyMap());
    }

    @Test
    void rechazaUnDestinoDeResetInvalido() {
        assertThatThrownBy(() -> new GrupoAdminService(admin).resetearOffsets(GRUPO, new ResetOffsetsRequest("medio", null)))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void reseteaAlPrincipioCuandoElGrupoEstaVacio() {
        grupoEnEstado(ConsumerGroupState.EMPTY);
        offsetsConfirmados(Map.of(P0, new OffsetAndMetadata(10), P1, new OffsetAndMetadata(7)));
        offsetsDelBroker(Map.of(P0, 25L, P1, 7L), Map.of(P0, 0L, P1, 0L));
        AlterConsumerGroupOffsetsResult alterado = mock(AlterConsumerGroupOffsetsResult.class);
        when(alterado.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(admin.alterConsumerGroupOffsets(eq(GRUPO), anyMap())).thenReturn(alterado);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<TopicPartition, OffsetAndMetadata>> captor = ArgumentCaptor.forClass(Map.class);

        List<OffsetParticionResponse> resultado = new GrupoAdminService(admin)
                .resetearOffsets(GRUPO, new ResetOffsetsRequest("earliest", null));

        verify(admin).alterConsumerGroupOffsets(eq(GRUPO), captor.capture());
        assertThat(captor.getValue().get(P0).offset()).isZero();
        assertThat(captor.getValue().get(P1).offset()).isZero();
        assertThat(resultado).extracting(OffsetParticionResponse::lag).containsExactly(25L, 7L);
    }
}
