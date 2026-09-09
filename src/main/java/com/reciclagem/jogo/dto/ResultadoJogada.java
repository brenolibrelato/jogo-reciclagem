package com.reciclagem.jogo.dto;

import com.reciclagem.jogo.model.RankingEntry;

import java.util.List;

public class ResultadoJogada {
    private final boolean acertou;
    private final int pontosGanhos;
    private final int pontuacaoTotal;
    private final int tentativaAtual;
    private final int pontosProximaTentativa;
    private final boolean jogoFinalizado;
    private final String mensagemFinal;
    private final long tempoTotalMs;
    private final List<RankingEntry> ranking;

    public ResultadoJogada(boolean acertou, int pontosGanhos, int pontuacaoTotal, int tentativaAtual,
                            int pontosProximaTentativa, boolean jogoFinalizado, String mensagemFinal,
                            long tempoTotalMs, List<RankingEntry> ranking) {
        this.acertou = acertou;
        this.pontosGanhos = pontosGanhos;
        this.pontuacaoTotal = pontuacaoTotal;
        this.tentativaAtual = tentativaAtual;
        this.pontosProximaTentativa = pontosProximaTentativa;
        this.jogoFinalizado = jogoFinalizado;
        this.mensagemFinal = mensagemFinal;
        this.tempoTotalMs = tempoTotalMs;
        this.ranking = ranking;
    }

    public boolean isAcertou() {
        return acertou;
    }

    public int getPontosGanhos() {
        return pontosGanhos;
    }

    public int getPontuacaoTotal() {
        return pontuacaoTotal;
    }

    public int getTentativaAtual() {
        return tentativaAtual;
    }

    public int getPontosProximaTentativa() {
        return pontosProximaTentativa;
    }

    public boolean isJogoFinalizado() {
        return jogoFinalizado;
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
