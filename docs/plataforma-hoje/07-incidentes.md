# Incidentes

*Registros do time de sistemas. A causa raiz e as ações são as que foram registradas na época.*

---

## INC-2024-117 - Lentidão e indisponibilidade na Black Friday

- **Data:** 29/11/2024, das 19h40 às 22h10
- **Impacto:** autorizações com mais de 30 s de resposta. Backoffice inacessível. Taxa de aprovação caiu de 86% para 41% no período. 2.140 transações ficaram `PENDENTE`.

**Linha do tempo**

| Hora | Evento |
|---|---|
| 19h40 | Suporte recebe as primeiras ligações de lojistas reclamando de lentidão |
| 19h55 | Plantão confirma CPU da app01 em 35%. Adquirente informa "instabilidade pontual" no lado dela |
| 20h10 | Primeiro reinício do Tomcat. Melhora por 5 minutos |
| 20h40 | Segundo reinício |
| 21h30 | Terceiro reinício |
| 22h10 | Volume cai e o sistema volta ao normal sozinho |

**Causa raiz registrada:** volume acima do previsto combinado com lentidão da adquirente.

**Ações**

- Aumentar a memória da app01 de 12 GB para 16 GB. *Feito.*
- Avaliar uma segunda instância do Tomcat para a próxima data de pico. *Ver INC-2025-021.*

---

## INC-2024-118 - Cobranças duplicadas depois da Black Friday

- **Data:** 30/11/2024 a 06/12/2024
- **Impacto:** 1.312 reclamações de consumidores. 1.188 pedidos com duas ou três transações aprovadas. R$ 412 mil estornados. 37 reclamações no Procon.

**O que aconteceu:** durante a lentidão de 29/11, vários lojistas reenviaram autorizações. O suporte orientou cada lojista a cancelar as transações duplicadas pela API. As que já estavam liquidadas foram debitadas pelo financeiro.

**Causa raiz registrada:** lojistas reenviaram requisições durante a lentidão.

**Ações**

- Comunicado aos lojistas pedindo para não reenviar requisições. *Feito em 10/12/2024.*

---

## INC-2025-021 - Liquidação em duplicidade com a segunda instância

- **Data:** 08/02/2025 a 11/02/2025
- **Impacto:** R$ 1,9 milhão pagos em duplicidade a 214 lojistas. A recuperação, debitando os repasses seguintes, levou 3 semanas.

**Linha do tempo**

| Data | Evento |
|---|---|
| 08/02 | Para preparar o Carnaval, a app02 é ligada e incluída no lb01 em balanceamento round-robin |
| 08/02 | Analistas reclamam que são deslogados a cada clique no backoffice. A infraestrutura configura `ip_hash` no NGINX e o problema some |
| 10/02 | Financeiro encontra arquivos de liquidação duplicados no SFTP para vários lojistas |
| 11/02 | A app02 é desligada e removida do lb01 |

**Causa raiz registrada:** configuração divergente da app02, que estava desatualizada em relação à app01.

**Ações**

- Proibido ligar a app02 sem aprovação da diretoria. *Feito.*
- Atualizar a app02. *Pendente.*

---

## INC-2024-063 - Diferenças de centavos na liquidação (recorrente)

- **Desde:** pelo menos 2019
- **Impacto:** diferenças de R$ 0,01 a R$ 0,09 por arquivo entre a soma das linhas e o total. Acumulado de 2024: R$ 3,8 mil.

**Causa raiz registrada:** arredondamento no extrato da adquirente.

**Ações**

- Ajuste manual mensal pela planilha `ajustes_liquidacao.xlsx`. *Em vigor.*

---

## INC-2025-044 - Queda do link MPLS com a adquirente

- **Data:** 14/04/2025, das 15h12 às 15h59
- **Impacto:** 3.906 transações `PENDENTE`. A conciliação com o extrato da adquirente levou 4 dias. 611 delas tinham sido aprovadas pela adquirente e foram corrigidas com `UPDATE` no banco. 23 consumidores foram cobrados sem que o lojista soubesse da aprovação.

**Causa raiz registrada:** rompimento de fibra da operadora do link.

**Ações**

- Contratar um segundo link MPLS. *Em orçamento.*

---

## INC-2025-052 - Arquivo de liquidação não gerado

- **Data:** 12/05/2025
- **Impacto:** 5 lojistas sem arquivo de liquidação no dia seguinte. Repasse atrasado em 1 dia.

**O que aconteceu:** às 23h de 11/05 o Tomcat foi reiniciado por causa de lentidão no backoffice. Na manhã seguinte faltavam arquivos. A liquidação foi executada pelo backoffice às 9h.

**Causa raiz registrada:** inconclusiva. O batch roda às 2h e o reinício foi às 23h.

---

## PRB-2024-009 - Aumento das revisões noturnas (problema em aberto)

- **Aberto em:** 06/2024
- **Descrição:** desde maio de 2024, a fila de revisão entre 21h e 3h ficou cerca de 4 vezes maior. A maioria das transações traz o motivo `HORARIO_MADRUGADA`. Os analistas encontram a fila cheia às 8h e muitos clientes desistem da compra.
- **Hipótese da equipe de risco:** aumento de tentativas de fraude no período noturno.
- **Situação:** sem causa identificada.
