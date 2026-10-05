package com.reciclagem.jogo.service;

import com.reciclagem.jogo.model.Dificuldade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.web.server.ResponseStatusException;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CachoeiraServiceTest {
    AtomicLong clock;
    RankingService ranking;
    CachoeiraService service;
    @BeforeEach void setup() {
        clock = new AtomicLong();
        ranking = mock(RankingService.class);
        service = new CachoeiraService(new JogoService(ranking, 15), ranking, clock::get, new Random(42));
    }
    String start() { return service.start("owner", "Ana", "FACIL").sessionId(); }
    void status(Runnable action, int code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(code));
    }
    @Test void rejectsMissingForeignAndUnknownSessions() {
        String id = start();
        status(() -> service.finish(null, id), 404);
        status(() -> service.finish("other", id), 404);
        status(() -> service.finish("owner", "invented"), 404);
        status(() -> service.finish("owner", null), 404);
        status(() -> service.command("other", id, "MOVER", 50d), 404);
        verifyNoInteractions(ranking);
    }
    @Test void rejectsEarlyFinishAndRequestFloodCannotAdvanceClock() {
        String id = start();
        for (int i = 0; i < 1000; i++) {
            assertThat(service.command("owner", id, "MOVER", 50d).tempoTotalMs()).isZero();
        }
        clock.set(59_999);
        status(() -> service.finish("owner", id), 409);
        verifyNoInteractions(ranking);
    }
    @Test void pauseUsesServerTimeAndExpiresEvenWhilePaused() {
        String id = start();
        clock.set(10_000);
        service.command("owner", id, "PAUSAR", 50d);
        clock.set(70_000);
        assertThat(service.command("owner", id, "CONTINUAR", 50d).tempoTotalMs()).isEqualTo(10_000);
        status(() -> service.finish("owner", id), 409);
        clock.set(900_000);
        status(() -> service.finish("owner", id), 410);
        verifyNoInteractions(ranking);
    }
    @Test void invalidControlsAndDifficultyCannotMutateGame() {
        status(() -> service.start("owner", "Ana", "fake"), 400);
        status(() -> service.start("owner", " ", "FACIL"), 400);
        String id = start();
        for (Double x : new Double[]{null, Double.NaN, Double.POSITIVE_INFINITY, -1d, 101d})
            status(() -> service.command("owner", id, "MOVER", x), 400);
        status(() -> service.command("owner", id, "PONTUAR", 50d), 400);
        verifyNoInteractions(ranking);
    }
    @Test void newGameInvalidatesPreviousAndCleanupRemovesExpired() {
        String old = start(); String current = start();
        status(() -> service.finish("owner", old), 404);
        clock.set(900_000); service.cleanup();
        status(() -> service.finish("owner", current), 404);
    }
    @ParameterizedTest @EnumSource(Dificuldade.class)
    void legitimateGameRecordsOnlyComputedScoreAndServerDuration(Dificuldade difficulty) {
        var state = service.start("owner", "Ana", difficulty.name());
        // An automated player still has to obey the authoritative simulation.
        for (int tick = 1; tick <= 1200; tick++) {
            double x = state.binX();
            var target = state.itens().stream().filter(f -> f.y() > 300).findFirst();
            if (target.isPresent()) x = (target.get().x() + 27) / 8;
            clock.set(tick * 50L);
            state = service.command("owner", state.sessionId(), "MOVER", x);
        }
        assertThat(state.status()).isEqualTo("fim");
        assertThat(state.tempoTotalMs()).isEqualTo(60_000);
        service.finish("owner", state.sessionId());
        verify(ranking).registrarPontuacao("cachoeira-lixo", difficulty, "Ana", state.pontuacao(), 60_000);
        String id = state.sessionId();
        status(() -> service.finish("owner", id), 409);
        status(() -> service.command("owner", id, "MOVER", 50d), 409);
    }
    @Test void catchesAndMissesAreComputedFromServerGeometry() {
        // Deterministic RNG: category papel, Jornal, x=373. Bin initially at 400.
        Random fixed = new Random() {
            @Override public int nextInt(int bound) { return bound == 25 ? 5 : 0; }
            @Override public double nextDouble() { return .5; }
        };
        service = new CachoeiraService(new JogoService(ranking, 15), ranking, clock::get, fixed);
        String id = start();
        clock.set(7000);
        assertThat(service.command("owner", id, "MOVER", 50d).pontuacao()).isEqualTo(10);
        // Move away: missed matching items deduct points instead of being omitted by the client.
        service.command("owner", id, "MOVER", 0d);
        clock.set(10_100);
        assertThat(service.command("owner", id, "MOVER", 0d).pontuacao()).isEqualTo(5);
        // Wrong category also uses server rules; client has no capture/score input.
        Random wrong = new Random() {
            @Override public int nextInt(int bound) { return 0; }
            @Override public double nextDouble() { return .5; }
        };
        clock.set(0);
        var other = new CachoeiraService(new JogoService(ranking, 15), ranking, clock::get, wrong);
        var s = other.start("owner", "Ana", "FACIL");
        clock.set(7000);
        assertThat(other.command("owner", s.sessionId(), "MOVER", 50d).pontuacao()).isZero();
    }
    @Test void concurrentFinishPersistsExactlyOnce() throws Exception {
        String id = start(); clock.set(60_000);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            var tasks = new java.util.ArrayList<Callable<Integer>>();
            for (int i = 0; i < 8; i++) tasks.add(() -> {
                try { service.finish("owner", id); return 200; }
                catch (ResponseStatusException ex) { return ex.getStatusCode().value(); }
            });
            int successful = 0;
            for (Future<Integer> result : executor.invokeAll(tasks)) if (result.get() == 200) successful++;
            assertThat(successful).isEqualTo(1);
            verify(ranking, times(1)).registrarPontuacao(eq("cachoeira-lixo"), eq(Dificuldade.FACIL), eq("Ana"), anyInt(), eq(60_000L));
        } finally { executor.shutdownNow(); }
    }
}
