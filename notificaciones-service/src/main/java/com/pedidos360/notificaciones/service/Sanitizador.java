package com.pedidos360.notificaciones.service;

public final class Sanitizador {

    private Sanitizador() {
    }

    public static String limpiar(String texto, int maximo) {
        if (texto == null) {
            return "";
        }
        String limpio = texto.replaceAll("[\\p{Cntrl}\\u2028\\u2029]+", " ").trim();
        if (limpio.length() <= maximo) {
            return limpio;
        }
        int corte = Character.isHighSurrogate(limpio.charAt(maximo - 1)) ? maximo - 1 : maximo;
        return limpio.substring(0, corte);
    }
}
