package com.reciclagem.jogo.model;

public class RankingEntry {
    private String nomeJogador;
    private int pontuacao;
    private long tempoTotalMs;
    private String dataHora;

    public RankingEntry() {
    }

    public RankingEntry(String nomeJogador, int pontuacao, long tempoTotalMs, String dataHora) {
        this.nomeJogador = nomeJogador;
        this.pontuacao = pontuacao;
        this.tempoTotalMs = tempoTotalMs;
        this.dataHora = dataHora;
    }

    public String getNomeJogador() {
        return nomeJogador;
    }

    public void setNomeJogador(String nomeJogador) {
        this.nomeJogador = nomeJogador;
    }

    public int getPontuacao() {
        return pontuacao;
    }

    public void setPontuacao(int pontuacao) {
        this.pontuacao = pontuacao;
    }

    public long getTempoTotalMs() {
        return tempoTotalMs;
    }

    public void setTempoTotalMs(long tempoTotalMs) {
        this.tempoTotalMs = tempoTotalMs;
    }

    public String getDataHora() {
        return dataHora;
    }

    public void setDataHora(String dataHora) {
        this.dataHora = dataHora;
    }
}
