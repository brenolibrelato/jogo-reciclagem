package com.reciclagem.jogo.model;

public enum Dificuldade {
    FACIL(30),
    MEDIO(15),
    DIFICIL(7);

    private final int tempoLimiteSegundos;

    Dificuldade(int tempoLimiteSegundos) {
        this.tempoLimiteSegundos = tempoLimiteSegundos;
    }

    public int getTempoLimiteSegundos() {
        return tempoLimiteSegundos;
    }

    public static Dificuldade fromTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return FACIL;
        }
        try {
            return Dificuldade.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return FACIL;
        }
    }
}
