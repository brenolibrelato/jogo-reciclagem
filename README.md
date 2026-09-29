# ♻️ Oficina de Reciclagem

Jogos educativos para ensinar crianças a separar corretamente cada tipo de resíduo, desenvolvidos para o **Projeto de Extensão Interdisciplinar II**.

**▶ Jogar:** https://jogo-reciclagem.onrender.com

> Hospedado no plano gratuito do Render: se o site estiver parado, o primeiro acesso pode levar cerca de um minuto para carregar.

## Os jogos

### Jogo de Reciclagem

A cada rodada aparece uma lixeira colorida e quatro objetos. A criança arrasta até a lixeira o objeto que pertence a ela antes que o tempo acabe.

- **10 rodadas** por partida
- Acertar logo vale mais: **10 pontos** na 1ª tentativa, **5** na 2ª, **2** na 3ª e **0** depois disso (máximo de 100 pontos)
- **3 dificuldades**, que mudam o tempo de cada rodada: Fácil (30 s), Médio (15 s) e Difícil (7 s)
- Aviso visual quando o tempo está acabando
- **Manual ilustrado** com as regras, a pontuação e o que vai em cada lixeira

### Cachoeira de Lixo

Os resíduos caem do alto da tela, e a criança move a lixeira para pegar só os que combinam com a categoria indicada. A categoria muda durante a partida, e a velocidade aumenta com o tempo.

- Partidas de **60 segundos**
- Controle pelo mouse, pelo toque (celular) ou pelas setas ← →
- **3 dificuldades** (velocidade inicial da queda) e botão de pausa
- Uma dica de reciclagem na tela final

Os dois jogos têm **ranking** (top 10 por dificuldade) e efeitos sonoros, com botão para desligar o som.

## As 5 lixeiras

| Cor | Material | Exemplos no jogo |
| --- | --- | --- |
| 🔵 Azul | Papel | jornal, revista, caixa de papelão |
| 🔴 Vermelho | Plástico | garrafa PET, copo descartável, sacola |
| 🟢 Verde | Vidro | garrafa de vidro, pote de geleia, jarra |
| 🟡 Amarelo | Metal | lata de refrigerante, lata de atum, fio de cobre |
| 🟤 Marrom | Orgânico | casca de banana, casca de ovo, resto de comida |

## Tecnologias

- **Back-end:** Java 17 e Spring Boot 3 (API REST)
- **Front-end:** HTML, CSS e JavaScript puro, sem frameworks
- **Deploy:** Docker no [Render](https://render.com)

As regras do Jogo de Reciclagem ficam no servidor: é ele que sorteia as rodadas, confere as jogadas e conta os pontos e o tempo, então a pontuação não pode ser alterada pelo navegador. A API também limita o número de requisições por IP, valida o nome do jogador e descarta partidas abandonadas.

## Como rodar localmente

Pré-requisitos: **Java 17** e **Maven**.

```bash
git clone https://github.com/brenolibrelato/jogo-reciclagem.git
cd jogo-reciclagem
mvn spring-boot:run
```

Depois é só abrir http://localhost:8080.

Ou, com Docker:

```bash
docker build -t jogo-reciclagem .
docker run -p 8080:8080 jogo-reciclagem
```

### Configuração

As opções ficam em `src/main/resources/application.properties` e podem ser sobrescritas por variáveis de ambiente:

| Propriedade | Padrão | Para que serve |
| --- | --- | --- |
| `server.port` | `8080` (ou `PORT`) | Porta do servidor |
| `app.cors.allowed-origins` | `http://localhost:8080` | Origens liberadas para a API (`APP_CORS_ALLOWED_ORIGINS` em produção) |
| `app.sessao.timeout-minutos` | `15` | Tempo até uma partida abandonada ser descartada |
| `app.ratelimit.max-requisicoes` | `60` | Requisições permitidas por IP em cada janela |
| `app.ratelimit.janela-segundos` | `60` | Duração da janela do limite de requisições |

O ranking é salvo em `data/ranking.json`. No plano gratuito do Render o disco não é permanente, então o ranking volta a zero quando o serviço é reiniciado.

## Estrutura

```
src/main/java/com/reciclagem/jogo/
├── controller/   # endpoints da API (/api/...)
├── service/      # regras do jogo e ranking
├── model/        # itens, lixeiras, dificuldade
├── dto/          # objetos de requisição e resposta
└── config/       # CORS e limite de requisições
src/main/resources/static/
├── index.html                    # menu de escolha do jogo
├── jogo-reciclagem.html          # Jogo de Reciclagem
├── cachoeira-lixo.html           # Cachoeira de Lixo
├── manual_jogo_reciclagem.html   # manual ilustrado
└── css/  images/  sounds/
```
