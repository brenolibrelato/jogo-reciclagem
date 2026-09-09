package com.reciclagem.jogo.dto;

import com.reciclagem.jogo.model.RankingEntry;

import java.util.List;

public class TempoEsgotadoResponse {
    private final boolean jogoFinalizado;
    private final int pontuacaoTotal;
    private final Rodada proximaRodada;
    private final String mensagemFinal;
    private final long tempoTotalMs;
    private final List<RankingEntry> ranking;

    public TempoEsgotadoResponse(boolean jogoFinalizado, int pontuacaoTotal, Rodada proximaRodada,
                                  String mensagemFinal, long tempoTotalMs, List<RankingEntry> ranking) {
        this.jogoFinalizado = jogoFinalizado;
        this.pontuacaoTotal = pontuacaoTotal;
        this.proximaRodada = proximaRodada;
        this.mensagemFinal = mensagemFinal;
        this.tempoTotalMs = tempoTotalMs;
        this.ranking = ranking;
    }

    public boolean isJogoFinalizado() {
        return jogoFinalizado;
    }

    public int getPontuacaoTotal() {
        return pontuacaoTotal;
    }

    public Rodada getProximaRodada() {
        return proximaRodada;
    }

    public String getMensagemFinal() {
        return mensagemFinal;
    }

    public long getTempoTotalMs() {
        return tempoTotalMs;
    }

    public List<RankingEntry> getRanking() {
        return ranking;
    }
}
