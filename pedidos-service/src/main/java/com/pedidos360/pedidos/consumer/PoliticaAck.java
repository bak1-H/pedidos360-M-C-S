package com.pedidos360.pedidos.consumer;

public final class PoliticaAck {

    private PoliticaAck() {
    }

    public static AccionAck decidir(Throwable falla, boolean redelivered) {
        if (falla == null) {
            return AccionAck.CONFIRMAR;
        }
        if (falla instanceof PayloadInvalidoException || falla instanceof Error) {
            return AccionAck.DESCARTAR;
        }
        return redelivered ? AccionAck.DESCARTAR : AccionAck.REENCOLAR;
    }
}
