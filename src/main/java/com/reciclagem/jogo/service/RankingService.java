package com.reciclagem.jogo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RankingService {

    public static final String JOGO_RECICLAGEM = "reciclagem";
    public static final String JOGO_CACHOEIRA_LIXO = "cachoeira-lixo";

    private static final int TAMANHO_RANKING = 10;
    private static final Path ARQUIVO_RANKING = Path.of("data", "ranking.json");

    private final ObjectMapper objectMapper;
    // jogo -> dificuldade -> ranking daquele jogo/dificuldade
    private final Map<String, Map<Dificuldade, List<RankingEntry>>> rankingsPorJogo = new ConcurrentHashMap<>();

    public RankingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public synchronized void carregar() {
        garantirEstrutura(JOGO_RECICLAGEM);
        garantirEstrutura(JOGO_CACHOEIRA_LIXO);

        if (!Files.exists(ARQUIVO_RANKING)) {
            return;
        }

        try {
            JsonNode raiz = objectMapper.readTree(ARQUIVO_RANKING.toFile());
            if (raiz == null || !raiz.fields().hasNext()) {
                return;
            }

            // Arquivos gravados antes do suporte a multiplos jogos guardavam
            // "dificuldade -> lista" diretamente. Detecta o formato para nao
            // perder um ranking ja existente em producao.
            if (formatoAntigoSemJogo(raiz)) {
                Map<String, List<RankingEntry>> salvo = objectMapper.convertValue(
                        raiz, new TypeReference<Map<String, List<RankingEntry>>>() {});
                salvo.forEach((chaveDificuldade, entradas) -> aplicarCarregado(JOGO_RECICLAGEM, chaveDificuldade, entradas));
            } else {
                Map<String, Map<String, List<RankingEntry>>> salvo = objectMapper.convertValue(
                        raiz, new TypeReference<Map<String, Map<String, List<RankingEntry>>>>() {});
                salvo.forEach((jogo, porDificuldade) ->
                        porDificuldade.forEach((chaveDificuldade, entradas) -> aplicarCarregado(jogo, chaveDificuldade, entradas)));
            }
        } catch (IOException ex) {
            // arquivo corrompido ou ilegível: mantém rankings vazios
        }
    }

    private boolean formatoAntigoSemJogo(JsonNode raiz) {
        Iterator<JsonNode> valores = raiz.elements();
        return valores.hasNext() && valores.next().isArray();
    }

    private void aplicarCarregado(String jogo, String chaveDificuldade, List<RankingEntry> entradas) {
        try {
            Dificuldade dificuldade = Dificuldade.valueOf(chaveDificuldade);
            garantirEstrutura(jogo);
            rankingsPorJogo.get(jogo).put(dificuldade, new ArrayList<>(entradas));
        } catch (IllegalArgumentException ignorado) {
            // dificuldade desconhecida no arquivo, ignora
        }
    }

    private void garantirEstrutura(String jogo) {
        rankingsPorJogo.computeIfAbsent(jogo, chave -> {
            Map<Dificuldade, List<RankingEntry>> mapa = new EnumMap<>(Dificuldade.class);
            for (Dificuldade dificuldade : Dificuldade.values()) {
                mapa.put(dificuldade, new ArrayList<>());
            }
            return mapa;
        });
    }

    public synchronized List<RankingEntry> registrarPontuacao(String jogo, Dificuldade dificuldade, String nomeJogador,
                                                                int pontuacao, long tempoTotalMs) {
        garantirEstrutura(jogo);
        String dataHora = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Map<Dificuldade, List<RankingEntry>> porDificuldade = rankingsPorJogo.get(jogo);
        List<RankingEntry> lista = porDificuldade.get(dificuldade);
        lista.add(new RankingEntry(nomeJogador, pontuacao, tempoTotalMs, dataHora));

        lista.sort(Comparator.comparingInt(RankingEntry::getPontuacao).reversed()
                .thenComparingLong(RankingEntry::getTempoTotalMs));

        if (lista.size() > TAMANHO_RANKING) {
            porDificuldade.put(dificuldade, new ArrayList<>(lista.subList(0, TAMANHO_RANKING)));
        }

        salvar();
        return obterRanking(jogo, dificuldade);
    }

    public synchronized List<RankingEntry> obterRanking(String jogo, Dificuldade dificuldade) {
        garantirEstrutura(jogo);
        return new ArrayList<>(rankingsPorJogo.get(jogo).get(dificuldade));
    }

    public synchronized Map<Dificuldade, List<RankingEntry>> obterTodosRankings(String jogo) {
        garantirEstrutura(jogo);
        return new EnumMap<>(rankingsPorJogo.get(jogo));
    }

    private void salvar() {
        try {
            Files.createDirectories(ARQUIVO_RANKING.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(ARQUIVO_RANKING.toFile(), rankingsPorJogo);
        } catch (IOException ex) {
            // falha ao persistir não deve derrubar o jogo; ranking permanece válido em memória
        }
    }
}
