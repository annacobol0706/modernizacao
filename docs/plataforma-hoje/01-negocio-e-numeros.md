# Negócio e números

## A empresa

A AnyCompany Meios de Pagamento Ltda é um gateway de pagamentos com sede em São Paulo, fundado em 2012. Ela conecta lojistas pequenos e médios a uma adquirente parceira e cuida de antifraude e liquidação. O lojista paga uma taxa por transação (MDR). A AnyCompany repassa à adquirente cerca de 1,6% e fica com a diferença.

Toda a operação roda num único sistema, o AnyCompany Gateway, instalado em VMs VMware num datacenter de colocation em São Paulo.

## Números dos últimos 12 meses

| Indicador | Valor |
|---|---|
| Lojistas ativos | 3.200 |
| Transações por dia (média) | 120 mil (1,4 por segundo) |
| Pico diário típico | 6 transações por segundo, entre 19h e 21h |
| Ticket médio | R$ 145 |
| Volume financeiro (TPV) por mês | R$ 520 milhões |
| MDR médio cobrado | 2,3% |
| Taxa de aprovação | 86% |
| Transações enviadas para revisão manual | 0,3% (cerca de 360 por dia) |
| Chargeback | 0,35% do TPV |
| Tamanho do banco | 1,8 TB (410 milhões de transações desde 2012) |

## Perfil dos lojistas

| Segmento | Lojistas | Transações | Ticket médio | Como integram |
|---|---|---|---|---|
| Varejo físico (padarias, mercados, restaurantes) | 72% | 55% | R$ 24 | PDV via integrador TEF |
| E-commerce | 24% | 35% | R$ 380 | Plugin de loja virtual ou SDK próprio |
| Turismo e serviços | 4% | 10% | R$ 2.200 | Integração própria |

No ambiente local, os lojistas Padaria Pao Quente, Loja Exemplo Eletronicos e Viagens Ceu Azul representam esses três segmentos.

## Sazonalidade

- **Black Friday:** o maior pico do ano. Em 2024 foram 640 mil transações no dia, com pico de 48 por segundo à 00h05 (abertura das promoções) e 35 por segundo às 20h.
- **Natal e Dia das Mães:** cerca de 2 vezes o volume de um dia normal.
- **Carnaval e férias de julho:** picos no segmento de turismo.

Para a Black Friday deste ano, o comercial projeta 1 milhão de transações no dia e pico de 80 por segundo, puxado por dois lojistas grandes de e-commerce que estão em negociação.

## SLAs contratados com os lojistas

| Item | Compromisso | Últimos 12 meses |
|---|---|---|
| Disponibilidade da API | 99,5% no mês | 99,2% na média. Novembro/2024 fechou em 97,8% |
| Tempo de resposta da autorização | p95 abaixo de 2 s | 1,1 s em dias normais. Acima de 30 s durante 2 horas na Black Friday |
| Arquivo de liquidação | Disponível no SFTP até 08h do dia seguinte (D+1) | 4 atrasos |
| Revisão manual | Concluída em até 30 min em horário comercial | Fila acumula durante a noite |

Em 2025 a AnyCompany pagou R$ 380 mil em multas contratuais por descumprimento de SLA.

## Custos de TI por ano

| Item | Custo anual |
|---|---|
| Colocation (espaço, energia, links de internet) | R$ 540 mil |
| Licenças VMware. A renovação proposta pela Broadcom sobe de R$ 180 mil para R$ 610 mil | R$ 180 mil (R$ 610 mil a partir da renovação) |
| SQL Server 2019 Standard (8 cores com Software Assurance) | R$ 140 mil |
| Link MPLS com a adquirente | R$ 96 mil |
| Suporte do hardware (3 servidores e storage de 2016) | R$ 85 mil |
| DBA terceirizado (40 horas por mês) | R$ 192 mil |

## Prazos que pressionam a decisão

- **Renovação do VMware:** em 6 meses, já com o preço novo.
- **Auditoria PCI DSS:** em 6 meses. A adquirente exige a certificação para renovar o contrato.
- **Contrato de colocation:** vence em 14 meses. A diretoria não quer renovar.
- **Fim do suporte do hardware:** o fabricante encerra o suporte dos servidores no fim do ano.
