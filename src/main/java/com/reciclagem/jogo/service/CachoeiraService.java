package com.reciclagem.jogo.service;

import com.reciclagem.jogo.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.*;
import java.util.function.LongSupplier;
import static org.springframework.http.HttpStatus.*;

/** Server-authoritative simulation. Clients supply controls, never outcomes or elapsed time. */
@Service
public class CachoeiraService {
    private static final long STEP = 50, DURATION = 60_000, TTL = 15 * 60_000;
    private static final int MAX_SESSIONS = 2000;
    private final Map<String, Session> sessions = new HashMap<>();
    private final JogoService catalog;
    private final RankingService ranking;
    private final LongSupplier clock;
    private final Random random;

    @Autowired
    public CachoeiraService(JogoService catalog, RankingService ranking) {
        this(catalog, ranking, () -> System.nanoTime() / 1_000_000, new SecureRandom());
    }

    CachoeiraService(JogoService catalog, RankingService ranking, LongSupplier clock, Random random) {
        this.catalog = catalog;
        this.ranking = ranking;
        this.clock = clock;
        this.random = random;
    }

    public record Falling(int id, Item item, double x, double y) {}
    public record State(String sessionId, String status, int pontuacao, long tempoTotalMs,
                        int categoriaIndex, double binX, List<Falling> itens) {}

    public synchronized State start(String owner, String name, String difficulty) {
        if (name == null || difficulty == null) throw badRequest();
        name = name.trim().replaceAll("\\p{Cntrl}", "");
        if (name.isBlank() || name.length() > 20) throw badRequest();
        Dificuldade level;
        try { level = Dificuldade.valueOf(difficulty); }
        catch (IllegalArgumentException ex) { throw badRequest(); }
        cleanup();
        sessions.values().removeIf(s -> s.owner.equals(owner));
        if (sessions.size() >= MAX_SESSIONS) throw new ResponseStatusException(TOO_MANY_REQUESTS);
        Session s = new Session(owner, name, level, clock.getAsLong(), random.nextInt(5));
        sessions.put(s.id, s);
        return state(s);
    }

    public synchronized State command(String owner, String id, String action, Double x) {
        if (action == null || !Set.of("MOVER", "PAUSAR", "CONTINUAR").contains(action)
                || x == null || !Double.isFinite(x) || x < 0 || x > 100) throw badRequest();
        Session s = session(owner, id);
        if (s.submitted) throw new ResponseStatusException(CONFLICT, "Partida já registrada");
        advance(s); // Simulate using PREVIOUS controls; new input cannot rewrite the past.
        if (s.elapsed < DURATION) {
            if (action.equals("PAUSAR")) s.paused = true;
            if (action.equals("CONTINUAR")) s.paused = false;
            s.x = Math.max(55, Math.min(745, x * 8));
        }
        return state(s);
    }

    public synchronized List<RankingEntry> finish(String owner, String id) {
        Session s = session(owner, id);
        if (s.submitted) throw new ResponseStatusException(CONFLICT, "Partida já registrada");
        advance(s);
        if (s.elapsed < DURATION) throw new ResponseStatusException(CONFLICT, "Partida ainda não terminou");
        // Lock covers validation, persistence and consumption, including concurrent requests.
        List<RankingEntry> result = ranking.registrarPontuacao(RankingService.JOGO_CACHOEIRA_LIXO,
                s.difficulty, s.name, s.score, s.elapsed);
        s.submitted = true;
        return result;
    }

    private Session session(String owner, String id) {
        Session s = id == null ? null : sessions.get(id);
        if (s == null || owner == null || !s.owner.equals(owner))
            throw new ResponseStatusException(NOT_FOUND, "Partida não encontrada");
        if (clock.getAsLong() - s.created >= TTL) {
            sessions.remove(id);
            throw new ResponseStatusException(GONE, "Partida expirada");
        }
        return s;
    }

    @Scheduled(fixedDelayString = "PT1M")
    public synchronized void cleanup() {
        long now = clock.getAsLong();
        sessions.values().removeIf(s -> now - s.created >= TTL);
    }

    private void advance(Session s) {
        long now = clock.getAsLong();
        if (!s.paused) s.pending += Math.max(0, now - s.last);
        s.last = now;
        while (s.pending >= STEP && s.elapsed < DURATION) {
            s.pending -= STEP;
            tick(s);
        }
    }

    private void tick(Session s) {
        s.elapsed += STEP;
        s.spawn += STEP;
        s.categoryTime += STEP;
        if (s.spawn >= s.spawnInterval) {
            s.spawn = 0;
            List<Item> items = catalog.getItensDisponiveis();
            s.items.add(new Falling(++s.nextId, items.get(random.nextInt(items.size())), random.nextDouble() * 746, -60));
        }
        List<Lixeira> bins = catalog.getLixeiras();
        for (int i = s.items.size() - 1; i >= 0; i--) {
            Falling old = s.items.get(i);
            Falling f = new Falling(old.id(), old.item(), old.x(), old.y() + s.speed * STEP / 1000);
            boolean correct = bins.get(s.category).aceitaItem(f.item());
            boolean caught = f.y() + 54 >= 422 && f.y() <= 492
                    && f.x() + 54 >= s.x - 55 && f.x() <= s.x + 55;
            if (caught || f.y() > 500) {
                int delta = caught ? (correct ? 10 : -5) : (correct ? -5 : 0);
                s.score = Math.max(0, s.score + delta);
                s.items.remove(i);
            } else s.items.set(i, f);
        }
        boolean near = s.items.stream().anyMatch(f -> f.y() > 360);
        if (s.categoryTime >= s.categoryInterval && (!near || s.categoryTime >= s.categoryInterval + 1000)) {
            s.category = (s.category + 1 + random.nextInt(4)) % 5;
            s.categoryTime = 0;
        }
        if (s.elapsed % 12_000 == 0) {
            s.speed = Math.min(s.speed * 1.12, 320);
            s.spawnInterval = Math.max(s.spawnInterval * .9, 550);
            s.categoryInterval = Math.max(s.categoryInterval - 1000, 11_000);
        }
    }

    private State state(Session s) {
        return new State(s.id, s.elapsed == DURATION ? "fim" : s.paused ? "pausado" : "jogando",
                s.score, s.elapsed, s.category, s.x / 8, List.copyOf(s.items));
    }

    private ResponseStatusException badRequest() { return new ResponseStatusException(BAD_REQUEST, "Dados inválidos"); }

    private static class Session {
        final String id = UUID.randomUUID().toString();
        final String owner, name;
        final Dificuldade difficulty;
        final long created;
        final List<Falling> items = new ArrayList<>();
        long last, elapsed, pending, spawn, categoryTime;
        double speed, spawnInterval, categoryInterval, x = 400;
        int category, score, nextId;
        boolean paused, submitted;
        Session(String owner, String name, Dificuldade difficulty, long now, int category) {
            this.owner = owner; this.name = name; this.difficulty = difficulty;
            this.created = now; this.last = now; this.category = category;
            speed = switch (difficulty) { case FACIL -> 80; case MEDIO -> 110; case DIFICIL -> 150; };
            spawnInterval = switch (difficulty) { case FACIL -> 1500; case MEDIO -> 1100; case DIFICIL -> 800; };
            categoryInterval = switch (difficulty) { case FACIL -> 24000; case MEDIO -> 18000; case DIFICIL -> 13000; };
        }
    }
}
