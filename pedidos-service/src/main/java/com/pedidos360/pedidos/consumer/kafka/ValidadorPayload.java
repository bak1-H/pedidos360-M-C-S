package com.pedidos360.pedidos.consumer.kafka;

import com.pedidos360.pedidos.consumer.PayloadInvalidoException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ValidadorPayload {

    private final Validator validator;

    public ValidadorPayload(Validator validator) {
        this.validator = validator;
    }

    public <T> T validar(T payload) {
        if (payload == null) {
            throw new PayloadInvalidoException("El cuerpo está vacío");
        }
        Set<ConstraintViolation<T>> violaciones = validator.validate(payload);
        if (!violaciones.isEmpty()) {
            throw new PayloadInvalidoException(
                    "Campos inválidos en " + payload.getClass().getSimpleName() + ": " + violaciones);
        }
        return payload;
    }
}
