package com.reciclagem.jogo.controller;

import com.reciclagem.jogo.dto.IniciarJogoRequest;
import com.reciclagem.jogo.dto.IniciarJogoResponse;
import com.reciclagem.jogo.dto.JogadaRequest;
import com.reciclagem.jogo.dto.ResultadoJogada;
import com.reciclagem.jogo.dto.Rodada;
import com.reciclagem.jogo.dto.SubmeterPontuacaoRequest;
import com.reciclagem.jogo.dto.TempoEsgotadoResponse;
import com.reciclagem.jogo.model.Dificuldade;
import com.reciclagem.jogo.model.Item;
import com.reciclagem.jogo.model.Lixeira;
import com.reciclagem.jogo.model.RankingEntry;
import com.reciclagem.jogo.service.JogoService;
import com.reciclagem.jogo.service.RankingService;
import com.reciclagem.jogo.service.SessaoNaoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class JogoApiController {

    private static final int NOME_JOGADOR_TAMANHO_MAXIMO = 20;
    private static final int PONTUACAO_MAXIMA_CACHOEIRA_LIXO = 5000;
    private static final long TEMPO_MAXIMO_CACHOEIRA_LIXO_MS = 180_000L;

    private final JogoService jogoService;
    private final RankingService rankingService;

    public JogoApiController(JogoService jogoService, RankingService rankingService) {
        this.jogoService = jogoService;
        this.rankingService = rankingService;
    }

    @GetMapping("/itens")
    public List<Item> getItens() {
        return jogoService.getItensDisponiveis();
    }

    @GetMapping("/lixeiras")
    public List<Lixeira> getLixeiras() {
        return jogoService.getLixeiras();
    }

    @PostMapping("/jogo/iniciar")
    public ResponseEntity<IniciarJogoResponse> iniciarJogo(@RequestBody IniciarJogoRequest request) {
        String nomeBruto = request.getNomeJogador();
        if (nomeBruto == null) {
            return ResponseEntity.badRequest().build();
        }

        String nome = nomeBruto.trim().replaceAll("\\p{Cntrl}", "");
        if (nome.isBlank() || nome.length() > NOME_JOGADOR_TAMANHO_MAXIMO) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(jogoService.iniciarJogo(nome, request.getDificuldade()));
    }

    @PostMapping("/jogo/{sessionId}/jogada")
    public ResponseEntity<ResultadoJogada> jogar(@PathVariable String sessionId, @RequestBody JogadaRequest request) {
        return ResponseEntity.ok(jogoService.processarJogada(sessionId, request.getItemId()));
    }

    @PostMapping("/jogo/{sessionId}/proxima-rodada")
    public ResponseEntity<Rodada> proximaRodada(@PathVariable String sessionId) {
        return ResponseEntity.ok(jogoService.avancarRodada(sessionId));
    }

    @PostMapping("/jogo/{sessionId}/tempo-esgotado")
    public ResponseEntity<TempoEsgotadoResponse> tempoEsgotado(@PathVariable String sessionId) {
        return ResponseEntity.ok(jogoService.tempoEsgotado(sessionId));
    }

    @GetMapping("/ranking/{dificuldade}")
    public ResponseEntity<List<RankingEntry>> getRankingPorDificuldade(@PathVariable String dificuldade) {
        return ResponseEntity.ok(rankingService.obterRanking(RankingService.JOGO_RECICLAGEM, Dificuldade.fromTexto(dificuldade)));
    }

    @GetMapping("/ranking")
    public ResponseEntity<Map<String, List<RankingEntry>>> getTodosRankings() {
        return ResponseEntity.ok(mapearRankingPorNomeDificuldade(RankingService.JOGO_RECICLAGEM));
    }

    @GetMapping("/ranking/cachoeira-lixo")
    public ResponseEntity<Map<String, List<RankingEntry>>> getTodosRankingsCachoeiraLixo() {
        return ResponseEntity.ok(mapearRankingPorNomeDificuldade(RankingService.JOGO_CACHOEIRA_LIXO));
    }

    @PostMapping("/ranking/cachoeira-lixo")
    public ResponseEntity<List<RankingEntry>> registrarPontuacaoCachoeiraLixo(@RequestBody SubmeterPontuacaoRequest request) {
        String nomeBruto = request.getNomeJogador();
        if (nomeBruto == null) {
            return ResponseEntity.badRequest().build();
        }

        String nome = nomeBruto.trim().replaceAll("\\p{Cntrl}", "");
        if (nome.isBlank() || nome.length() > NOME_JOGADOR_TAMANHO_MAXIMO) {
            return ResponseEntity.badRequest().build();
        }

        Dificuldade dificuldade = Dificuldade.fromTexto(request.getDificuldade());
        int pontuacao = Math.max(0, Math.min(request.getPontuacao(), PONTUACAO_MAXIMA_CACHOEIRA_LIXO));
        long tempoTotalMs = Math.max(0, Math.min(request.getTempoTotalMs(), TEMPO_MAXIMO_CACHOEIRA_LIXO_MS));

        List<RankingEntry> ranking = rankingService.registrarPontuacao(
                RankingService.JOGO_CACHOEIRA_LIXO, dificuldade, nome, pontuacao, tempoTotalMs);
        return ResponseEntity.ok(ranking);
    }

    private Map<String, List<RankingEntry>> mapearRankingPorNomeDificuldade(String jogo) {
        Map<String, List<RankingEntry>> resposta = new LinkedHashMap<>();
        rankingService.obterTodosRankings(jogo).forEach((dificuldade, entradas) -> resposta.put(dificuldade.name(), entradas));
        return resposta;
    }

    @ExceptionHandler(SessaoNaoEncontradaException.class)
    public ResponseEntity<String> tratarSessaoNaoEncontrada(SessaoNaoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
