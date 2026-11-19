package com.pedidos360.kafkaadmin.service;

import com.pedidos360.kafkaadmin.config.RecursosProtegidosProperties;
import com.pedidos360.kafkaadmin.dto.AumentarParticionesRequest;
import com.pedidos360.kafkaadmin.dto.CambiarConfiguracionRequest;
import com.pedidos360.kafkaadmin.dto.CrearTopicoRequest;
import com.pedidos360.kafkaadmin.dto.ParticionResponse;
import com.pedidos360.kafkaadmin.dto.PatronesKafka;
import com.pedidos360.kafkaadmin.dto.TopicoDetalleResponse;
import com.pedidos360.kafkaadmin.dto.TopicoResumenResponse;
import com.pedidos360.kafkaadmin.exception.RecursoProtegidoException;
import com.pedidos360.kafkaadmin.exception.SolicitudInvalidaException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.pedidos360.kafkaadmin.service.EsperaKafka.esperar;

@Service
public class TopicoAdminService {

    static final int PARTICIONES_POR_DEFECTO = 3;
    static final short REPLICAS_POR_DEFECTO = 3;

    private final Admin admin;
    private final RecursosProtegidosProperties recursosProtegidos;

    public TopicoAdminService(Admin admin, RecursosProtegidosProperties recursosProtegidos) {
        this.admin = admin;
        this.recursosProtegidos = recursosProtegidos;
    }

    public List<TopicoResumenResponse> listar() {
        Set<String> nombres = esperar(admin.listTopics(new ListTopicsOptions().listInternal(false)).names());
        if (nombres.isEmpty()) {
            return List.of();
        }
        return esperar(admin.describeTopics(nombres).allTopicNames()).values().stream()
                .sorted(Comparator.comparing(TopicDescription::name))
                .map(this::resumir)
                .toList();
    }

    public TopicoDetalleResponse obtener(String nombre) {
        validarNombre(nombre);
        TopicDescription descripcion = describir(nombre);
        return new TopicoDetalleResponse(
                descripcion.name(),
                recursosProtegidos.esProtegido(nombre),
                descripcion.partitions().stream().map(this::particion).toList(),
                configuracionDe(nombre));
    }

    public TopicoResumenResponse crear(CrearTopicoRequest request) {
        validarNombre(request.nombre());
        Map<String, String> configuracion = request.configuracion() == null ? Map.of() : request.configuracion();
        validarClaves(configuracion.keySet());
        int particiones = request.particiones() == null ? PARTICIONES_POR_DEFECTO : request.particiones();
        short replicas = request.replicas() == null ? REPLICAS_POR_DEFECTO : request.replicas();

        esperar(admin.createTopics(List.of(new NewTopic(request.nombre(), particiones, replicas).configs(configuracion))).all());

        return new TopicoResumenResponse(request.nombre(), particiones, replicas, recursosProtegidos.esProtegido(request.nombre()));
    }

    public TopicoDetalleResponse cambiarConfiguracion(String nombre, CambiarConfiguracionRequest request) {
        validarNombre(nombre);
        Map<String, String> establecer = request.establecer() == null ? Map.of() : request.establecer();
        List<String> eliminar = request.eliminar() == null ? List.of() : request.eliminar();
        if (establecer.isEmpty() && eliminar.isEmpty()) {
            throw new SolicitudInvalidaException("Debe indicar al menos una clave en establecer o eliminar");
        }
        validarClaves(establecer.keySet());
        validarClaves(eliminar);

        List<AlterConfigOp> operaciones = new ArrayList<>();
        establecer.forEach((clave, valor) -> operaciones.add(
                new AlterConfigOp(new ConfigEntry(clave, valor), AlterConfigOp.OpType.SET)));
        eliminar.forEach(clave -> operaciones.add(
                new AlterConfigOp(new ConfigEntry(clave, null), AlterConfigOp.OpType.DELETE)));
        esperar(admin.incrementalAlterConfigs(Map.of(recurso(nombre), operaciones)).all());

        return obtener(nombre);
    }

    public TopicoDetalleResponse aumentarParticiones(String nombre, AumentarParticionesRequest request) {
        validarNombre(nombre);
        int actuales = describir(nombre).partitions().size();
        if (request.total() <= actuales) {
            throw new SolicitudInvalidaException(
                    "total debe ser mayor que las particiones actuales (" + actuales + ")");
        }
        esperar(admin.createPartitions(Map.of(nombre, NewPartitions.increaseTo(request.total()))).all());
        return obtener(nombre);
    }

    public void eliminar(String nombre) {
        validarNombre(nombre);
        exigirNoProtegido(nombre);
        describir(nombre);
        esperar(admin.deleteTopics(List.of(nombre)).all());
    }

    private TopicDescription describir(String nombre) {
        return esperar(admin.describeTopics(List.of(nombre)).allTopicNames()).get(nombre);
    }

    private Map<String, String> configuracionDe(String nombre) {
        Config config = esperar(admin.describeConfigs(List.of(recurso(nombre))).all()).get(recurso(nombre));
        Map<String, String> configuracion = new TreeMap<>();
        config.entries().stream()
                .filter(entrada -> entrada.source() == ConfigEntry.ConfigSource.DYNAMIC_TOPIC_CONFIG)
                .forEach(entrada -> configuracion.put(entrada.name(), entrada.value()));
        return configuracion;
    }

    private ConfigResource recurso(String nombre) {
        return new ConfigResource(ConfigResource.Type.TOPIC, nombre);
    }

    private TopicoResumenResponse resumir(TopicDescription descripcion) {
        int replicas = descripcion.partitions().isEmpty() ? 0 : descripcion.partitions().get(0).replicas().size();
        return new TopicoResumenResponse(descripcion.name(), descripcion.partitions().size(), replicas,
                recursosProtegidos.esProtegido(descripcion.name()));
    }

    private ParticionResponse particion(TopicPartitionInfo info) {
        return new ParticionResponse(info.partition(),
                info.leader() == null ? null : info.leader().id(),
                info.replicas().stream().map(Node::id).toList(),
                info.isr().stream().map(Node::id).toList());
    }

    private void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new SolicitudInvalidaException("nombre es obligatorio");
        }
        if (!nombre.matches(PatronesKafka.NOMBRE) || nombre.equals(".") || nombre.equals("..")) {
            throw new SolicitudInvalidaException(
                    "nombre solo admite letras, números, punto, guion y guion bajo (máximo 249)");
        }
    }

    private void validarClaves(Iterable<String> claves) {
        claves.forEach(clave -> {
            if (clave == null || clave.isBlank()) {
                throw new SolicitudInvalidaException("Las claves de configuración no pueden estar vacías");
            }
        });
    }

    private void exigirNoProtegido(String nombre) {
        if (recursosProtegidos.esProtegido(nombre)) {
            throw new RecursoProtegidoException("El tópico " + nombre + " está protegido y no se puede eliminar");
        }
    }
}
