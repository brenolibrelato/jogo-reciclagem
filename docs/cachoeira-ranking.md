# Ranking autoritativo — Cachoeira de Lixo

## Problema e solução

O POST antigo aceitava nome, dificuldade, pontuação e tempo escolhidos pelo navegador.
Limitar esses números não comprovava o resultado. Agora o servidor mantém a partida,
sorteia os resíduos/categorias e simula queda, colisões, penalidades e progressão em passos
fixos de 50 ms, usando relógio monotônico. Só os controles vêm do cliente. Entradas novas
não alteram passos passados. Enviar muitas requisições não acelera o relógio.

Regras preservadas: 60 segundos ativos; +10 por captura correta; -5 por captura errada
ou perda de item da categoria atual; piso zero; três dificuldades; progressão a cada
12 segundos; troca de categoria com tolerância de 1 segundo quando há item próximo.
A área lógica é 800 × 500, escalada para a tela: tamanho de viewport ou zoom não muda
as regras. O navegador consulta o servidor aproximadamente a cada 100 ms, sem requisições
sobrepostas. Tempo de rede influencia a resposta aos controles; requer conexão durante a partida.

## Contrato HTTP

| Operação | Corpo | Resultado |
| --- | --- | --- |
| `POST /api/cachoeira-lixo/iniciar` | `{"nomeJogador":"Ana","dificuldade":"FACIL"}` | Estado e `sessionId`; cookie HttpOnly identifica o navegador |
| `POST /api/cachoeira-lixo/{sessionId}/comando` | `{"acao":"MOVER","binX":50}` | Estado calculado; ações também podem ser `PAUSAR` e `CONTINUAR` |
| `POST /api/ranking/cachoeira-lixo` | `{"sessionId":"..."}` | Grava uma única vez o resultado calculado; retorna ranking |
| `GET /api/ranking/cachoeira-lixo` | — | Leitura existente, preservada |

`binX` é uma posição horizontal de 0 a 100; a simulação limita a lixeira às bordas.
O endpoint de ranking rejeita campos adicionais, inclusive os quatro campos antigos.
O nome e a dificuldade ficam fixos desde o início. Não há endpoint para declarar capturas,
criar resíduos, mudar tempo ou informar pontos.

- 400: corpo/campos inválidos ou dificuldade desconhecida.
- 404: partida inexistente ou pertencente a outro navegador (inclusive sem cookie).
- 409: tentativa de registrar antes do fim, registrar novamente ou jogar após registro.
- 410: partida expirada; após limpeza, 404.
- 429: limite de requisições ou capacidade de partidas atingido.

Só uma partida por navegador fica válida; começar outra invalida a anterior. Expiração
absoluta de 15 minutos, inclusive pausada; limpeza a cada minuto; máximo de 2.000 partidas
em memória. Validação, simulação e consumo são sincronizados, impedindo duplicação por
requisições concorrentes. Pausas repetidas são idempotentes e não alteram o tempo já jogado.

## Validação

```bash
mvn test
mvn spring-boot:run
```

Abra `http://localhost:8080/cachoeira-lixo.html`:

1. Jogue nas três dificuldades; use mouse, toque e setas. Confira mudança de categoria,
   pontuação, queda e dificuldade progressiva.
2. Pause por alguns segundos e retome: o relógio ativo deve continuar de onde parou.
3. Termine os 60 segundos. Confira o resultado e o ranking; jogue novamente.
4. Nas ferramentas de rede, confira que o POST final contém somente `sessionId`.
5. Reenvie esse POST: deve retornar 409, sem duplicar. Em outro navegador/sem cookie,
   deve retornar 404. Antes do término, com cookie correto, deve retornar 409.
6. Altere `estado.pontuacao` no console: o próximo estado recebido corrige a exibição e
   o ranking permanece calculado no servidor. Adicione `pontuacao` ao POST final: 400.
7. Desconecte a rede durante a partida: a interface informa a interrupção e permite
   iniciar novamente; não envia resultado local como alternativa.

Exemplo do ataque original, agora rejeitado com 400:

```bash
curl -i http://localhost:8080/api/ranking/cachoeira-lixo \
  -H 'Content-Type: application/json' \
  -d '{"nomeJogador":"Fraude","dificuldade":"DIFICIL","pontuacao":5000,"tempoTotalMs":0}'
```

Os testes usam relógio controlado, sem esperar uma partida real. Cobrem proprietário,
campos forjados, replay, concorrência, conclusão precoce, expiração, pausa, coordenadas
inválidas, física/pontos e partidas válidas nas três dificuldades. O teste HTTP usa o
controller e o serviço reais, com somente o armazenamento do ranking simulado.

## Operação e limites

- Publique backend e HTML juntos. O cliente antigo deixa de conseguir registrar resultados.
- O limite original de início/ranking continua separado dos comandos. Com a configuração
  padrão, comandos têm orçamento de 900 requisições por 60 segundos por endereço remoto.
  Não se confia diretamente em `X-Forwarded-For`. Atrás de proxy, configure a infraestrutura
  confiável para normalizar o endereço remoto; sem isso, usuários podem compartilhar limite.
- Cookies só são transportados por cookie, não por URL, com HttpOnly e SameSite=Strict.
  Sirva produção por HTTPS e configure `SERVER_SERVLET_SESSION_COOKIE_SECURE=true`.
- O serviço usa memória local, como o jogo existente. Reinício invalida partidas; múltiplas
  réplicas exigem afinidade de sessão ou armazenamento compartilhado com transações.
- O armazenamento JSON do ranking permanece existente, inclusive sua limitação de disco
  efêmero e tratamento de erro de gravação. Não se promete durabilidade além dessa estrutura.
- Entradas antigas não foram apagadas e não podem ser comprovadas retroativamente.
  Avalie um novo ranking/temporada antes de considerar todo o histórico verificado.
- A correção impede declarar resultados arbitrários, mas não identifica pessoas nem
  impede bots que enviem controles válidos. A sessão não prova presença humana.
- O outro jogo (`JogoService`, ranking `reciclagem`) não foi reformulado: suas validações
  de rodada, concorrência e tempo merecem uma revisão separada. Seus resultados não
  permitem registrar no ranking da Cachoeira.
