package com.reciclagem.jogo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reciclagem.jogo.model.Dificuldade;
import com.reciclagem.jogo.model.RankingEntry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class RankingService {

    private static final int TAMANHO_RANKING = 10;
    private static final Path ARQUIVO_RANKING = Path.of("data", "ranking.json");

    private final ObjectMapper objectMapper;
    private final Map<Dificuldade, List<RankingEntry>> rankings = new EnumMap<>(Dificuldade.class);

    public RankingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public synchronized void carregar() {
        for (Dificuldade dificuldade : Dificuldade.values()) {
            rankings.put(dificuldade, new ArrayList<>());
        }

        if (!Files.exists(ARQUIVO_RANKING)) {
            return;
        }

        try {
            Map<String, List<RankingEntry>> salvo = objectMapper.readValue(
                    ARQUIVO_RANKING.toFile(), new TypeReference<Map<String, List<RankingEntry>>>() {});

            salvo.forEach((chave, entradas) -> {
                try {
                    rankings.put(Dificuldade.valueOf(chave), new ArrayList<>(entradas));
                } catch (IllegalArgumentException ignorado) {
                    // dificuldade desconhecida no arquivo, ignora
                }
            });
        } catch (IOException ex) {
            // arquivo corrompido ou ilegível: mantém rankings vazios
        }
    }

    public synchronized List<RankingEntry> registrarPontuacao(Dificuldade dificuldade, String nomeJogador,
                                                                int pontuacao, long tempoTotalMs) {
        String dataHora = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        List<RankingEntry> lista = rankings.get(dificuldade);
        lista.add(new RankingEntry(nomeJogador, pontuacao, tempoTotalMs, dataHora));

        lista.sort(Comparator.comparingInt(RankingEntry::getPontuacao).reversed()
                .thenComparingLong(RankingEntry::getTempoTotalMs));

        if (lista.size() > TAMANHO_RANKING) {
            rankings.put(dificuldade, new ArrayList<>(lista.subList(0, TAMANHO_RANKING)));
        }

        salvar();
        return obterRanking(dificuldade);
    }

    public synchronized List<RankingEntry> obterRanking(Dificuldade dificuldade) {
        return new ArrayList<>(rankings.get(dificuldade));
    }

    public synchronized Map<Dificuldade, List<RankingEntry>> obterTodosRankings() {
        return new EnumMap<>(rankings);
    }

    private void salvar() {
        try {
            Files.createDirectories(ARQUIVO_RANKING.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(ARQUIVO_RANKING.toFile(), rankings);
        } catch (IOException ex) {
            // falha ao persistir não deve derrubar o jogo; ranking permanece válido em memória
        }
    }
}
