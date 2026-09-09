package com.reciclagem.jogo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Aplicacao {
    public static void main(String[] args) {
        SpringApplication.run(Aplicacao.class, args);
        System.out.println("✅ Jogo da Reciclagem rodando em: http://localhost:8080");
        System.out.println("📦 API disponível em: http://localhost:8080/api/itens");
    }
}