package com.reciclagem.jogo.dto;

import com.reciclagem.jogo.model.Item;
import com.reciclagem.jogo.model.Lixeira;

import java.util.List;

public class Rodada {
    private final int numeroRodada;
    private final int totalRodadas;
    private final Lixeira lixeira;
    private final List<Item> opcoes;
    private final int tentativaAtual;
    private final int pontosProximaTentativa;
    private final String dificuldade;
    private final int tempoLimiteSegundos;

    public Rodada(int numeroRodada, int totalRodadas, Lixeira lixeira, List<Item> opcoes,
                   int tentativaAtual, int pontosProximaTentativa, String dificuldade, int tempoLimiteSegundos) {
        this.numeroRodada = numeroRodada;
        this.totalRodadas = totalRodadas;
        this.lixeira = lixeira;
        this.opcoes = opcoes;
        this.tentativaAtual = tentativaAtual;
        this.pontosProximaTentativa = pontosProximaTentativa;
        this.dificuldade = dificuldade;
        this.tempoLimiteSegundos = tempoLimiteSegundos;
    }

    public int getNumeroRodada() {
        return numeroRodada;
    }

    public int getTotalRodadas() {
        return totalRodadas;
    }

    public Lixeira getLixeira() {
        return lixeira;
    }

    public List<Item> getOpcoes() {
        return opcoes;
    }

    public int getTentativaAtual() {
        return tentativaAtual;
    }

    public int getPontosProximaTentativa() {
        return pontosProximaTentativa;
    }

    public String getDificuldade() {
        return dificuldade;
    }

    public int getTempoLimiteSegundos() {
        return tempoLimiteSegundos;
    }
}
