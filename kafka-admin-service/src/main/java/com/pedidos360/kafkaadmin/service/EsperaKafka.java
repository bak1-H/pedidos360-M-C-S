package com.pedidos360.kafkaadmin.service;

import com.pedidos360.kafkaadmin.exception.ConflictoException;
import com.pedidos360.kafkaadmin.exception.RecursoNoEncontradoException;
import com.pedidos360.kafkaadmin.exception.SolicitudInvalidaException;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class EsperaKafka {

    private static final long TIEMPO_MAXIMO_SEGUNDOS = 15;

    private EsperaKafka() {
    }

    static <T> T esperar(KafkaFuture<T> futuro) {
        try {
            return futuro.get(TIEMPO_MAXIMO_SEGUNDOS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new KafkaException("La operación fue interrumpida", ex);
        } catch (TimeoutException ex) {
            throw new org.apache.kafka.common.errors.TimeoutException("Kafka no respondió a tiempo", ex);
        } catch (ExecutionException ex) {
            throw traducir(ex.getCause());
        }
    }

    private static RuntimeException traducir(Throwable causa) {
        if (causa instanceof UnknownTopicOrPartitionException) {
            return new RecursoNoEncontradoException("El tópico no existe");
        }
        if (causa instanceof GroupIdNotFoundException) {
            return new RecursoNoEncontradoException("El grupo no existe");
        }
        if (causa instanceof TopicExistsException) {
            return new ConflictoException("El tópico ya existe");
        }
        if (causa instanceof InvalidTopicException
                || causa instanceof InvalidPartitionsException
                || causa instanceof InvalidReplicationFactorException
                || causa instanceof InvalidConfigurationException) {
            return new SolicitudInvalidaException(causa.getMessage());
        }
        if (causa instanceof KafkaException kafkaException) {
            return kafkaException;
        }
        return new KafkaException(causa);
    }
}
