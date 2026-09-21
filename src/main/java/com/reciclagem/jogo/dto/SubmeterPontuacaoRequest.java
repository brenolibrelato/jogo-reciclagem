package com.reciclagem.jogo.dto;

public class SubmeterPontuacaoRequest {
    private String nomeJogador;
    private String dificuldade;
    private int pontuacao;
    private long tempoTotalMs;

    public String getNomeJogador() {
        return nomeJogador;
    }

    public void setNomeJogador(String nomeJogador) {
        this.nomeJogador = nomeJogador;
    }

    public String getDificuldade() {
        return dificuldade;
    }

    public void setDificuldade(String dificuldade) {
        this.dificuldade = dificuldade;
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
}
