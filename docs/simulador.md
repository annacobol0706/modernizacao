# Simulador de uso

O simulador faz o papel dos lojistas: autoriza, captura, cancela e consulta transações com o mesmo comportamento das integrações reais descritas em [quem usa a plataforma](plataforma-hoje/02-quem-usa.md), incluindo timeouts e reenvios. Ele também abre a tela de transações do backoffice a cada 10 segundos, como um analista faria, e mede quanto tempo ela leva.

Use o simulador para ver os problemas acontecendo e para medir o antes e o depois da sua modernização.

O simulador é uma ferramenta do laboratório, não faz parte do gateway da AnyCompany. Ele lê o banco direto para descobrir as API keys e montar o relatório de sintomas.

## Como rodar

O container `simulador` sobe junto com o resto e fica esperando comandos:

```bash
docker compose exec simulador simulador <comando>
```

Troque `docker compose` por `finch compose` se estiver usando Finch. Com o override de Apple Silicon, inclua os mesmos `-f` que você usou no `up`. Pare qualquer cenário com Ctrl+C: ele termina as compras em andamento e mostra o resumo.

## Comandos

| Comando | O que faz |
|---|---|
| `dia-normal [--tps 1.5] [--minutos N]` | Tráfego de um dia comum até você parar. De tempos em tempos aparece um fraudador testando um cartão roubado |
| `black-friday [--tps-pico 12] [--fator-tempo 1.0]` | Cerca de 9 minutos: aquecimento, rampa, pico com a adquirente instável, fim do pico e recuperação. No fim mostra os sintomas. `--fator-tempo 0.5` faz tudo na metade do tempo |
| `adquirente normal\|lenta\|instavel\|fora` | Muda o comportamento da adquirente simulada até você mudar de novo ou reiniciar o container `adquirente` |
| `sintomas` | Lê o banco e mostra o que se acumulou: pedidos cobrados duas vezes, transações pendentes, fila de revisão, diferenças na liquidação |

Modos da adquirente:

| Modo | Comportamento |
|---|---|
| `normal` | Responde em cerca de 250 ms |
| `lenta` | Aprovações levam cerca de 2,5 s |
| `instavel` | Como na Black Friday de 2024: algumas chamadas levam 25 s, algumas falham com HTTP 503, o resto fica mais lento |
| `fora` | Derruba toda conexão, como na queda do link MPLS (INC-2025-044) |

## Lendo o painel

A cada 10 segundos aparece uma linha assim:

```
15:18:45  compras/s 10.5 | aprov  58  neg   6  rev   0  pend  4  erro  0 | timeout   3  retry   3  desist  0 | p50  2.5s  p95  4.3s | em voo  57 | backoffice esperando ha 11s
```

| Campo | Significado |
|---|---|
| compras/s | Compras novas iniciadas pelos lojistas nos últimos 10 s |
| aprov, neg, rev, pend, erro | Respostas recebidas pelos lojistas, por status |
| timeout | Chamadas em que o lojista desistiu de esperar |
| retry | Reenvios feitos pelos lojistas depois de um timeout |
| desist | Compras em que o lojista esgotou as tentativas e mostrou erro para o cliente |
| p50, p95 | Tempo de resposta das autorizações que voltaram |
| em voo | Compras em andamento no momento |
| backoffice | Quanto tempo a tela de transações levou para abrir, ou há quanto tempo está esperando |

## Roteiro sugerido

1. **Dia normal.** Rode `dia-normal` por 10 minutos com o backoffice aberto no navegador. Veja as transações chegando, a fila de revisão e o tempo do backoffice.
2. **Black Friday.** Rode `black-friday` e acompanhe o painel. Compare com o [INC-2024-117](plataforma-hoje/07-incidentes.md) e o [INC-2024-118](plataforma-hoje/07-incidentes.md). Enquanto roda, tente usar o backoffice.
3. **Queda da adquirente.** Com `dia-normal` rodando em um terminal, rode `adquirente fora` em outro por 1 minuto e depois `adquirente normal`. Rode `sintomas`.
4. **Liquidação.** Clique em **Executar liquidação agora** no backoffice, abra um arquivo gerado e confira a linha `TOTAL`:
   ```bash
   docker compose exec app ls /usr/local/tomcat/liquidacao
   docker compose exec app cat /usr/local/tomcat/liquidacao/<arquivo>.csv
   ```
5. **Duas instâncias.** Reproduza o [INC-2025-021](plataforma-hoje/07-incidentes.md):
   ```bash
   docker compose -f docker-compose.yml -f docker-compose.duas-instancias.yml up --build -d
   docker compose -f docker-compose.yml -f docker-compose.duas-instancias.yml exec simulador simulador dia-normal --minutos 6
   docker compose -f docker-compose.yml -f docker-compose.duas-instancias.yml exec simulador simulador sintomas
   ```
   Nesse cenário a liquidação roda a cada 2 minutos e o backoffice fica atrás do balanceador em http://localhost:8081/anycompany. Para voltar ao normal:
   ```bash
   docker compose -f docker-compose.yml -f docker-compose.duas-instancias.yml down
   docker compose up -d
   ```

Para recomeçar do zero, com o banco limpo: `docker compose down -v` e `docker compose up -d`.

## Usando contra a sua versão modernizada

O tráfego do simulador só depende do contrato SOAP. Se a sua nova arquitetura mantiver esse contrato, aponte o simulador para ela mudando `ANYCOMPANY_URL` no serviço `simulador` do compose e rode os mesmos cenários. Os números do `black-friday` antes e depois são uma boa evidência para a sua apresentação.

Duas partes dependem do banco atual: a leitura das API keys e o relatório de sintomas. Se você mudar o banco, adapte `simulador/pagsim/banco.py`. O código do simulador fica em `simulador/pagsim/`.
