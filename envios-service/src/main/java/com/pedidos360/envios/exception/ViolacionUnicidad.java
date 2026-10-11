package com.pedidos360.envios.exception;

import org.hibernate.exception.ConstraintViolationException;

import java.sql.SQLException;
import java.util.Set;

public final class ViolacionUnicidad {

    private static final String SQL_STATE_UNICIDAD = "23505";
    private static final Set<Integer> CODIGOS_SQL_SERVER = Set.of(2601, 2627);

    private ViolacionUnicidad() {
    }

    public static boolean esDeUnicidad(Throwable falla) {
        for (Throwable actual = falla; actual != null; actual = actual.getCause()) {
            if (actual instanceof ConstraintViolationException violacion
                    && violacion.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE) {
                return true;
            }
            if (actual instanceof SQLException sql && esCodigoDeUnicidad(sql)) {
                return true;
            }
            if (actual.getCause() == actual) {
                return false;
            }
        }
        return false;
    }

    private static boolean esCodigoDeUnicidad(SQLException sql) {
        return SQL_STATE_UNICIDAD.equals(sql.getSQLState()) || CODIGOS_SQL_SERVER.contains(sql.getErrorCode());
    }
}
