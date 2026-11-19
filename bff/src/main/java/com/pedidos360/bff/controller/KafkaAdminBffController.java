package com.pedidos360.bff.controller;

import com.pedidos360.bff.client.KafkaAdminClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/bff/kafka-admin")
@RequiredArgsConstructor
public class KafkaAdminBffController {

    private static final String BASE = "/internal/kafka-admin";

    private final KafkaAdminClient kafkaAdminClient;

    @GetMapping("/topicos")
    public Object listarTopicos() {
        return kafkaAdminClient.consultar(BASE + "/topicos");
    }

    @GetMapping("/topicos/{nombre}")
    public Object obtenerTopico(@PathVariable String nombre) {
        return kafkaAdminClient.consultar(BASE + "/topicos/{nombre}", nombre);
    }

    @PostMapping("/topicos")
    public ResponseEntity<Object> crearTopico(@RequestBody Map<String, Object> request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(kafkaAdminClient.crear(BASE + "/topicos", request));
    }

    @PutMapping("/topicos/{nombre}/configuracion")
    public Object cambiarConfiguracion(@PathVariable String nombre, @RequestBody Map<String, Object> request) {
        return kafkaAdminClient.actualizar(BASE + "/topicos/{nombre}/configuracion", request, nombre);
    }

    @PostMapping("/topicos/{nombre}/particiones")
    public Object aumentarParticiones(@PathVariable String nombre, @RequestBody Map<String, Object> request) {
        return kafkaAdminClient.crear(BASE + "/topicos/{nombre}/particiones", request, nombre);
    }

    @DeleteMapping("/topicos/{nombre}")
    public ResponseEntity<Void> eliminarTopico(@PathVariable String nombre) {
        kafkaAdminClient.eliminar(BASE + "/topicos/{nombre}", nombre);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/grupos")
    public Object listarGrupos() {
        return kafkaAdminClient.consultar(BASE + "/grupos");
    }

    @GetMapping("/grupos/{id}")
    public Object obtenerGrupo(@PathVariable String id) {
        return kafkaAdminClient.consultar(BASE + "/grupos/{id}", id);
    }

    @PostMapping("/grupos/{id}/reset-offsets")
    public Object resetearOffsets(@PathVariable String id, @RequestBody Map<String, Object> request) {
        return kafkaAdminClient.crear(BASE + "/grupos/{id}/reset-offsets", request, id);
    }

    @GetMapping("/cluster")
    public Object describirCluster() {
        return kafkaAdminClient.consultar(BASE + "/cluster");
    }
}
