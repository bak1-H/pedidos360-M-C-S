package com.pedidos360.kafkaadmin.service;

import com.pedidos360.kafkaadmin.config.RecursosProtegidosProperties;
import com.pedidos360.kafkaadmin.dto.AumentarParticionesRequest;
import com.pedidos360.kafkaadmin.dto.CrearTopicoRequest;
import com.pedidos360.kafkaadmin.dto.TopicoDetalleResponse;
import com.pedidos360.kafkaadmin.exception.RecursoNoEncontradoException;
import com.pedidos360.kafkaadmin.exception.RecursoProtegidoException;
import com.pedidos360.kafkaadmin.exception.SolicitudInvalidaException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopicoAdminServiceTest {

    @Mock
    private Admin admin;

    private final RecursosProtegidosProperties protegidos = new RecursosProtegidosProperties(
            List.of("pedidos360.pedidos.eventos", "pedidos360.pedidos.eventos.DLT",
                    "pedidos360.envios.tracking", "pedidos360.envios.tracking.DLT"),
            List.of("__"));

    private TopicoAdminService service() {
        return new TopicoAdminService(admin, protegidos);
    }

    private TopicDescription descripcion(String nombre, int particiones) {
        Node nodo = new Node(1, "kafka1", 9092);
        List<TopicPartitionInfo> info = java.util.stream.IntStream.range(0, particiones)
                .mapToObj(i -> new TopicPartitionInfo(i, nodo, List.of(nodo), List.of(nodo)))
                .toList();
        return new TopicDescription(nombre, false, info);
    }

    private void topicoExiste(String nombre, int particiones) {
        DescribeTopicsResult resultado = mock(DescribeTopicsResult.class);
        when(resultado.allTopicNames()).thenReturn(KafkaFuture.completedFuture(Map.of(nombre, descripcion(nombre, particiones))));
        when(admin.describeTopics(List.of(nombre))).thenReturn(resultado);
    }

    private void topicoInexistente(String nombre) {
        KafkaFutureImpl<Map<String, TopicDescription>> falla = new KafkaFutureImpl<>();
        falla.completeExceptionally(new UnknownTopicOrPartitionException());
        DescribeTopicsResult resultado = mock(DescribeTopicsResult.class);
        when(resultado.allTopicNames()).thenReturn(falla);
        when(admin.describeTopics(List.of(nombre))).thenReturn(resultado);
    }

    @Test
    void creaTopicoConValoresPorDefectoYConfiguracion() {
        CreateTopicsResult resultado = mock(CreateTopicsResult.class);
        when(resultado.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(admin.createTopics(any(Collection.class))).thenReturn(resultado);
        ArgumentCaptor<Collection<NewTopic>> captor = ArgumentCaptor.forClass(Collection.class);

        service().crear(new CrearTopicoRequest("prueba.topico", null, null, Map.of("retention.ms", "1000")));

        verify(admin).createTopics(captor.capture());
        NewTopic creado = captor.getValue().iterator().next();
        assertThat(creado.name()).isEqualTo("prueba.topico");
        assertThat(creado.numPartitions()).isEqualTo(3);
        assertThat(creado.replicationFactor()).isEqualTo((short) 3);
        assertThat(creado.configs()).containsEntry("retention.ms", "1000");
    }

    @Test
    void rechazaNombresInvalidosSinLlamarAlBroker() {
        assertThatThrownBy(() -> service().crear(new CrearTopicoRequest(" ", 3, (short) 3, null)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("nombre es obligatorio");
        assertThatThrownBy(() -> service().crear(new CrearTopicoRequest("con espacios", 3, (short) 3, null)))
                .isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> service().crear(new CrearTopicoRequest("ok", 3, (short) 3, Map.of(" ", "1"))))
                .isInstanceOf(SolicitudInvalidaException.class);

        verify(admin, never()).createTopics(any(Collection.class));
    }

    @Test
    void creaTopicoQueYaExisteDevuelveConflicto() {
        KafkaFutureImpl<Void> falla = new KafkaFutureImpl<>();
        falla.completeExceptionally(new TopicExistsException("ya existe"));
        CreateTopicsResult resultado = mock(CreateTopicsResult.class);
        when(resultado.all()).thenReturn(falla);
        when(admin.createTopics(any(Collection.class))).thenReturn(resultado);

        assertThatThrownBy(() -> service().crear(new CrearTopicoRequest("duplicado", 3, (short) 3, null)))
                .isInstanceOf(com.pedidos360.kafkaadmin.exception.ConflictoException.class);
    }

    @Test
    void noEliminaTopicosDeNegocioNiInternos() {
        for (String nombre : List.of("pedidos360.pedidos.eventos", "pedidos360.pedidos.eventos.DLT",
                "pedidos360.envios.tracking", "pedidos360.envios.tracking.DLT", "__consumer_offsets")) {
            assertThatThrownBy(() -> service().eliminar(nombre)).isInstanceOf(RecursoProtegidoException.class);
        }

        verify(admin, never()).deleteTopics(any(Collection.class));
    }

    @Test
    void eliminaTopicoExistenteYFallaSiNoExiste() {
        topicoExiste("temporal", 3);
        DeleteTopicsResult resultado = mock(DeleteTopicsResult.class);
        when(resultado.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(admin.deleteTopics(List.of("temporal"))).thenReturn(resultado);
        topicoInexistente("fantasma");

        service().eliminar("temporal");

        verify(admin).deleteTopics(List.of("temporal"));
        assertThatThrownBy(() -> service().eliminar("fantasma")).isInstanceOf(RecursoNoEncontradoException.class);
        verify(admin, never()).deleteTopics(List.of("fantasma"));
    }

    @Test
    void soloAumentaParticionesSiElTotalEsMayor() {
        topicoExiste("temporal", 3);

        assertThatThrownBy(() -> service().aumentarParticiones("temporal", new AumentarParticionesRequest(3)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("3");

        verify(admin, never()).createPartitions(any());
    }

    @Test
    void detalleIncluyeParticionesYSoloLaConfiguracionDelTopico() {
        topicoExiste("pedidos360.envios.tracking", 2);
        ConfigResource recurso = new ConfigResource(ConfigResource.Type.TOPIC, "pedidos360.envios.tracking");
        Config config = new Config(List.of(
                new ConfigEntry("retention.ms", "259200000", ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG, false, false, List.of(), ConfigEntry.ConfigType.LONG, null),
                new ConfigEntry("segment.bytes", "1073741824", ConfigEntry.ConfigSource.DEFAULT_CONFIG, false, false, List.of(), ConfigEntry.ConfigType.LONG, null)));
        DescribeConfigsResult resultado = mock(DescribeConfigsResult.class);
        when(resultado.all()).thenReturn(KafkaFuture.completedFuture(Map.of(recurso, config)));
        when(admin.describeConfigs(List.of(recurso))).thenReturn(resultado);

        TopicoDetalleResponse detalle = service().obtener("pedidos360.envios.tracking");

        assertThat(detalle.protegido()).isTrue();
        assertThat(detalle.particiones()).hasSize(2);
        assertThat(detalle.particiones().get(0).lider()).isEqualTo(1);
        assertThat(detalle.configuracion()).containsOnlyKeys("retention.ms");
    }
}
