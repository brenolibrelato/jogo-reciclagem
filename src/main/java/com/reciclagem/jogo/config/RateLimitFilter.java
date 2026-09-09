package com.reciclagem.jogo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final int maxRequisicoes;
    private final long janelaMs;
    private final Map<String, Contador> contadoresPorIp = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${app.ratelimit.max-requisicoes}") int maxRequisicoes,
                            @Value("${app.ratelimit.janela-segundos}") long janelaSegundos) {
        this.maxRequisicoes = maxRequisicoes;
        this.janelaMs = janelaSegundos * 1000L;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String ip = obterIpCliente(request);
        Contador contador = contadoresPorIp.computeIfAbsent(ip, chave -> new Contador());

        if (!contador.tentarRegistrar(maxRequisicoes, janelaMs)) {
            response.setStatus(429);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Muitas requisições. Tente novamente em instantes.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Scheduled(fixedDelayString = "PT10M")
    void limparContadoresInativos() {
        long limite = System.currentTimeMillis() - (janelaMs * 5);
        contadoresPorIp.entrySet().removeIf(entrada -> entrada.getValue().inicioJanela < limite);
    }

    private String obterIpCliente(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            return encaminhado.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class Contador {
        private int quantidade = 0;
        private long inicioJanela = System.currentTimeMillis();

        synchronized boolean tentarRegistrar(int maxRequisicoes, long janelaMs) {
            long agora = System.currentTimeMillis();
            if (agora - inicioJanela > janelaMs) {
                inicioJanela = agora;
                quantidade = 0;
            }
            quantidade++;
            return quantidade <= maxRequisicoes;
        }
    }
}
