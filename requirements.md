# Requisitos de Negócio — Modernização do Serviço de Lançamentos Financeiros

**Versão:** 0.3  
**Status:** Aprovado para design  
**Data:** 14/10/2026  

> **Contexto:** Este documento descreve os requisitos de negócio de um projeto **educacional** que simula a modernização de uma funcionalidade bancária legada. As regras aqui definidas são premissas específicas deste projeto e não devem ser interpretadas como regras universais do sistema financeiro brasileiro.

---

## Histórico de Revisões

| Versão | Data | Alterações |
|--------|------|-----------|
| 0.1-draft | 14/10/2026 | Versão inicial. |
| 0.2 | 14/10/2026 | Incorporação das decisões V1–V8: contas via carga inicial, auditabilidade sem identificação do solicitante, precisão monetária BRL/2 casas, concorrência, ordenação do extrato, validação de entrada, formato UTC/ms e paginação/filtro de período. |
| 0.3 | 14/10/2026 | Resolução das questões Q1–Q3: descrição opcional (0–200 chars, trim), paginação (padrão 20, máximo 100), filtro por timestamp interno UTC com intervalo fechado. Questões abertas encerradas. |

---

## 1. Introdução e Problema de Negócio

Sistemas bancários legados baseados em mainframe concentram lógica crítica de negócio acumulada ao longo de décadas. Essa concentração cria obstáculos crescentes: dificuldade de manutenção, dependência de profissionais especializados em tecnologias antigas, alto custo operacional e limitações para evolução do produto.

Este projeto simula um cenário real de modernização: a extração de uma funcionalidade específica do mainframe — o processamento de lançamentos financeiros e a consulta de extrato — para um serviço independente, com comportamento equivalente ao sistema original.

O problema central a ser resolvido é garantir que essa extração preserve dois pilares fundamentais da operação bancária:

- **Consistência dos lançamentos:** toda movimentação registrada deve refletir corretamente no saldo da conta, sem perdas, duplicações ou inconsistências, inclusive sob operações concorrentes.
- **Rastreabilidade das operações:** cada lançamento deve ser identificável, auditável e consultável, reproduzindo a capacidade de extrato do sistema legado.

---

## 2. Objetivo do Serviço

O serviço tem como objetivo registrar lançamentos de crédito e débito em contas fictícias predefinidas, manter o saldo atualizado a cada operação e disponibilizar o extrato de movimentações por conta, com suporte a filtro por período e paginação.

O escopo é educacional e simula condições reais de modernização bancária, sem integração com sistemas financeiros reais.

---

## 3. Escopo

### 3.1 Incluído

- Registro de lançamentos de **crédito** (entrada de valor) em uma conta predefinida.
- Registro de lançamentos de **débito** (saída de valor) em uma conta predefinida.
- Consulta do **saldo atual** de uma conta.
- Consulta do **extrato** de uma conta, com suporte a filtro por período e paginação.
- Rastreabilidade de cada lançamento: identificador único, tipo, valor, descrição, data/hora de registro e resultado do processamento.
- Carga inicial de contas fictícias predefinidas com saldos iniciais definidos.
- Garantia de que operações concorrentes não resultem em saldo negativo indevido.
- Proteção contra registro duplicado de uma mesma solicitação *(idempotência)*.

### 3.2 Excluído

- Criação, encerramento ou alteração de dados cadastrais de contas.
- Transferências entre contas (operação que envolve duas contas simultaneamente).
- Autenticação, autorização e identificação do solicitante.
- Integração com sistemas bancários externos ou reais.
- Cálculo de juros, tarifas ou qualquer regra financeira derivada.
- Notificações (e-mail, SMS, push).
- Estorno ou cancelamento de lançamentos já registrados.
- Suporte a múltiplas moedas.

---

## 4. Premissas Confirmadas

> As premissas abaixo são decisões específicas deste projeto educacional, confirmadas durante o levantamento de requisitos. Não representam regras universais do sistema financeiro.

| # | Tema | Decisão |
|---|------|---------|
| P1 | Saldo negativo | **Este projeto não permite saldo negativo.** Um débito que excederia o saldo disponível deve ser rejeitado. A conta não é alterada em caso de rejeição. |
| P2 | Valor dos lançamentos | Cada lançamento deve ter valor em BRL, positivo, maior que zero, com no máximo duas casas decimais. Valores com mais de duas casas decimais devem ser rejeitados — sem arredondamento automático. |
| P3 | Ordenação do extrato | O extrato é ordenado pela data/hora de registro atribuída pelo serviço, da mais antiga para a mais recente. Em caso de empate de timestamp, o critério de desempate é o identificador único do lançamento, em ordem crescente. |
| P4 | Identificação de contas | Uma conta é identificada por um código único (número de conta). Não há agência neste escopo. |
| P5 | Carga inicial de contas | As contas fictícias e seus saldos iniciais são definidos por carga inicial de dados. O saldo inicial pode ser diferente de zero. O cadastro e a criação de contas estão fora do escopo do serviço. |
| P6 | Formato de data/hora | Todas as datas e horas são registradas e exibidas no formato ISO 8601 com referência UTC e precisão de milissegundos. Exemplo: `2026-10-14T17:30:00.123Z`. |
| P7 | Limites de valor | Não há limite mínimo ou máximo de valor por lançamento além da regra de saldo disponível (P1) e da precisão monetária (P2). |
| P8 | Idempotência | Uma solicitação repetida com o mesmo identificador de requisição não deve registrar o lançamento novamente. O serviço deve reconhecer a repetição e retornar o resultado da operação original. |
| P9 | Campo descrição | O campo descrição é **opcional**. Quando informado, deve ter entre 1 e 200 caracteres após a remoção de espaços nas extremidades (*trim*). Uma descrição composta apenas de espaços em branco deve ser rejeitada. A omissão do campo não invalida o lançamento. |
| P10 | Paginação do extrato | O tamanho padrão de página é 20 lançamentos. O tamanho máximo é 100 lançamentos por página. Solicitações com tamanho de página superior a 100, igual a zero ou negativo devem ser rejeitadas. Número de página inválido (zero, negativo ou não inteiro) deve ser rejeitado. |
| P11 | Filtro de período do extrato | O filtro de período utiliza o timestamp de registro atribuído pelo serviço ao lançamento. Os valores de início e fim informados pelo consumidor são interpretados em UTC. O intervalo é fechado: lançamentos com timestamp igual ao início ou ao fim são incluídos no resultado. |

> **Questões abertas:** Todas as questões (Q1–Q3) foram resolvidas nesta versão. Não há questões pendentes.

---

## 5. Requisitos Funcionais

### RF-01 — Registrar Lançamento de Crédito

O serviço deve permitir registrar um lançamento de crédito em uma conta predefinida, informando o valor e, opcionalmente, uma descrição.

**Regras de negócio:**

- **Conta:** deve existir na carga inicial; caso contrário, a operação é rejeitada com erro de conta não encontrada.
- **Valor:** deve ser em BRL, positivo, maior que zero, com no máximo duas casas decimais. Campo obrigatório. Qualquer violação rejeita a operação.
- **Descrição:** campo opcional. Se informada, deve ter entre 1 e 200 caracteres após *trim*. Descrição composta apenas de espaços em branco deve ser rejeitada.
- **Campos obrigatórios ausentes:** campos obrigatórios ausentes, nulos ou de tipo incompatível rejeitam a operação com mensagem de erro identificando o campo.
- **Após registro bem-sucedido:** o saldo da conta é atualizado somando o valor creditado.
- **Identificação do lançamento:** cada lançamento recebe identificador único e data/hora de registro atribuídos pelo serviço no momento do processamento, em ISO 8601 UTC com precisão de milissegundos.
- **Idempotência:** solicitação repetida com o mesmo identificador de requisição retorna o resultado da operação original sem registrar novo lançamento.

---

### RF-02 — Registrar Lançamento de Débito

O serviço deve permitir registrar um lançamento de débito em uma conta predefinida, informando o valor e, opcionalmente, uma descrição.

**Regras de negócio:**

- **Conta:** deve existir na carga inicial; caso contrário, a operação é rejeitada com erro de conta não encontrada.
- **Valor:** deve ser em BRL, positivo, maior que zero, com no máximo duas casas decimais. Campo obrigatório. Qualquer violação rejeita a operação.
- **Descrição:** campo opcional. Se informada, deve ter entre 1 e 200 caracteres após *trim*. Descrição composta apenas de espaços em branco deve ser rejeitada.
- **Campos obrigatórios ausentes:** campos obrigatórios ausentes, nulos ou de tipo incompatível rejeitam a operação com mensagem de erro identificando o campo.
- **Saldo suficiente:** o débito só é registrado se o saldo disponível cobrir integralmente o valor no momento do processamento. Este projeto não permite saldo negativo.
- **Saldo insuficiente:** a operação é rejeitada e nenhuma alteração ocorre na conta.
- **Após registro bem-sucedido:** o saldo da conta é atualizado subtraindo o valor debitado.
- **Identificação do lançamento:** cada lançamento recebe identificador único e data/hora de registro atribuídos pelo serviço, em ISO 8601 UTC com precisão de milissegundos.
- **Idempotência:** solicitação repetida com o mesmo identificador de requisição retorna o resultado da operação original sem registrar novo lançamento.
- **Concorrência:** operações concorrentes sobre a mesma conta não podem produzir saldo negativo. Dois débitos simultâneos podem ser ambos aceitos somente se o saldo for suficiente para cada um após a consideração consistente dos lançamentos efetivados.

---

### RF-03 — Consultar Saldo

O serviço deve permitir consultar o saldo atual de uma conta existente.

**Regras de negócio:**

- A conta informada deve existir na carga inicial; caso contrário, a operação é rejeitada.
- O saldo retornado deve refletir todos os lançamentos registrados até o momento da consulta.
- A consulta não deve alterar nenhum dado da conta.

---

### RF-04 — Consultar Extrato

O serviço deve permitir consultar o extrato de uma conta existente, retornando a lista paginada de lançamentos registrados, com suporte a filtro opcional por período.

**Regras de negócio:**

- **Conta:** deve existir na carga inicial; caso contrário, a operação é rejeitada.
- **Conteúdo por lançamento:** identificador único, tipo (crédito ou débito), valor em BRL, descrição (quando presente) e data/hora de registro em ISO 8601 UTC com precisão de milissegundos.
- **Ordenação:** por data/hora de registro, do mais antigo para o mais recente. Empate de timestamp: desempate pelo identificador único em ordem crescente.
- **Filtro de período:** parâmetro opcional. Quando informado, deve conter data/hora de início e fim em ISO 8601 UTC. Apenas lançamentos com timestamp de registro dentro do intervalo fechado [início, fim] são retornados. Data fim anterior à data início é rejeitada.
- **Paginação:** obrigatória. Tamanho padrão: 20 lançamentos por página. Tamanho máximo: 100 lançamentos por página. Tamanho de página superior a 100, igual a zero ou negativo é rejeitado. Número de página zero, negativo ou não inteiro é rejeitado. A resposta deve indicar o total de lançamentos disponíveis (considerando o filtro aplicado) e se há páginas adicionais.
- **Não alteração:** a consulta não deve modificar nenhum dado da conta ou dos lançamentos.

---

## 6. Histórias de Usuário e Critérios de Aceitação

> Os critérios de aceitação utilizam o formato **EARS (Easy Approach to Requirements Syntax)**:
> - **Ubíquo:** O sistema deve [ação].
> - **Orientado a evento:** Quando [gatilho], o sistema deve [ação].
> - **Condicional:** Enquanto [estado], quando [gatilho], o sistema deve [ação].
> - **Indesejado:** Se [condição indesejada], o sistema deve [resposta].

---

### US-01 — Registrar crédito em conta

**Como** operador do sistema,  
**quero** registrar um lançamento de crédito em uma conta,  
**para que** o saldo da conta seja atualizado e a operação fique registrada no histórico com rastreabilidade.

**Critérios de aceitação:**

| # | Cenário | Critério (EARS) |
|---|---------|-----------------|
| CA-01.1 | Crédito válido sem descrição | Quando um crédito com valor positivo e no máximo duas casas decimais for registrado em uma conta existente sem descrição, o sistema deve confirmar o registro e retornar o identificador único do lançamento e a data/hora de registro em ISO 8601 UTC com precisão de milissegundos. |
| CA-01.2 | Crédito válido com descrição | Quando um crédito válido for registrado com descrição entre 1 e 200 caracteres (após trim), o sistema deve aceitar a operação e incluir a descrição no registro do lançamento. |
| CA-01.3 | Atualização de saldo | Quando um crédito for registrado com sucesso, o saldo consultado imediatamente após deve ser exatamente igual ao saldo anterior somado ao valor creditado. |
| CA-01.4 | Valor zero | Se o valor informado for zero, o sistema deve rejeitar a operação, retornar mensagem de erro indicando que o valor deve ser maior que zero, e não alterar o saldo da conta. |
| CA-01.5 | Valor negativo | Se o valor informado for negativo, o sistema deve rejeitar a operação, retornar mensagem de erro indicando que o valor deve ser positivo, e não alterar o saldo da conta. |
| CA-01.6 | Precisão excedida | Se o valor informado tiver mais de duas casas decimais (ex.: 10.999), o sistema deve rejeitar a operação, retornar mensagem de erro indicando a precisão máxima permitida de duas casas decimais, e não alterar o saldo da conta. |
| CA-01.7 | Valor não numérico ou ausente | Se o valor informado não for um número, for nulo ou estiver ausente, o sistema deve rejeitar a operação e retornar mensagem de erro identificando o campo inválido. |
| CA-01.8 | Descrição apenas com espaços | Se a descrição informada for composta exclusivamente de espaços em branco, o sistema deve rejeitar a operação e retornar mensagem de erro indicando que o campo descrição não pode ser vazio. |
| CA-01.9 | Descrição excede 200 caracteres | Se a descrição informada, após trim, tiver mais de 200 caracteres, o sistema deve rejeitar a operação e retornar mensagem de erro indicando o limite máximo permitido. |
| CA-01.10 | Conta inexistente | Se a conta informada não existir na carga inicial, o sistema deve rejeitar a operação e retornar mensagem de erro indicando que a conta não foi encontrada. |
| CA-01.11 | Solicitação repetida — operação original aceita | Se uma solicitação com o mesmo identificador de requisição for enviada mais de uma vez e a operação original tiver sido aceita, o sistema deve retornar o resultado original (identificador do lançamento, data/hora de registro) sem registrar novo lançamento nem alterar o saldo novamente. |
| CA-01.12 | Solicitação repetida — operação original rejeitada | Se uma solicitação com o mesmo identificador de requisição for enviada mais de uma vez e a operação original tiver sido rejeitada, o sistema deve retornar o mesmo resultado de rejeição sem efeito colateral. |

---

### US-02 — Registrar débito em conta

**Como** operador do sistema,  
**quero** registrar um lançamento de débito em uma conta,  
**para que** o saldo da conta seja atualizado e a operação fique registrada no histórico com rastreabilidade.

**Critérios de aceitação:**

| # | Cenário | Critério (EARS) |
|---|---------|-----------------|
| CA-02.1 | Débito válido sem descrição | Quando um débito com valor positivo, no máximo duas casas decimais e saldo suficiente for registrado sem descrição, o sistema deve confirmar o registro e retornar o identificador único e a data/hora de registro em ISO 8601 UTC com precisão de milissegundos. |
| CA-02.2 | Débito válido com descrição | Quando um débito válido for registrado com descrição entre 1 e 200 caracteres (após trim), o sistema deve aceitar a operação e incluir a descrição no registro. |
| CA-02.3 | Atualização de saldo | Quando um débito for registrado com sucesso, o saldo consultado imediatamente após deve ser exatamente igual ao saldo anterior subtraído do valor debitado. |
| CA-02.4 | Saldo exatamente suficiente | Quando o valor do débito for exatamente igual ao saldo disponível, o sistema deve registrar o débito e o saldo resultante deve ser exatamente zero. |
| CA-02.5 | Saldo insuficiente | Se o valor do débito for maior que o saldo disponível da conta, o sistema deve rejeitar a operação, retornar mensagem de erro indicando saldo insuficiente, e não alterar o saldo da conta. |
| CA-02.6 | Valor zero | Se o valor informado for zero, o sistema deve rejeitar a operação, retornar mensagem de erro indicando que o valor deve ser maior que zero, e não alterar o saldo da conta. |
| CA-02.7 | Valor negativo | Se o valor informado for negativo, o sistema deve rejeitar a operação, retornar mensagem de erro indicando que o valor deve ser positivo, e não alterar o saldo da conta. |
| CA-02.8 | Precisão excedida | Se o valor informado tiver mais de duas casas decimais, o sistema deve rejeitar a operação, retornar mensagem de erro indicando a precisão máxima de duas casas decimais, e não alterar o saldo da conta. |
| CA-02.9 | Valor não numérico ou ausente | Se o valor informado não for um número, for nulo ou estiver ausente, o sistema deve rejeitar a operação e retornar mensagem de erro identificando o campo inválido. |
| CA-02.10 | Descrição apenas com espaços | Se a descrição informada for composta exclusivamente de espaços em branco, o sistema deve rejeitar a operação e retornar mensagem de erro indicando que o campo descrição não pode ser vazio. |
| CA-02.11 | Descrição excede 200 caracteres | Se a descrição informada, após trim, tiver mais de 200 caracteres, o sistema deve rejeitar a operação e retornar mensagem de erro indicando o limite máximo permitido. |
| CA-02.12 | Conta inexistente | Se a conta informada não existir na carga inicial, o sistema deve rejeitar a operação e retornar mensagem de erro indicando que a conta não foi encontrada. |
| CA-02.13 | Solicitação repetida — operação original aceita | Se uma solicitação com o mesmo identificador de requisição for enviada mais de uma vez e a operação original tiver sido aceita, o sistema deve retornar o resultado original sem registrar novo lançamento nem alterar o saldo novamente. |
| CA-02.14 | Solicitação repetida — operação original rejeitada | Se uma solicitação com o mesmo identificador de requisição for enviada mais de uma vez e a operação original tiver sido rejeitada, o sistema deve retornar o mesmo resultado de rejeição sem efeito colateral. |
| CA-02.15 | Concorrência — saldo suficiente apenas para um | Se dois débitos forem solicitados simultaneamente para a mesma conta e o saldo disponível for suficiente apenas para um deles, o sistema deve aceitar exatamente um débito e rejeitar o outro com mensagem de saldo insuficiente. O saldo resultante não deve ser negativo. |
| CA-02.16 | Concorrência — saldo suficiente para ambos | Se dois débitos forem solicitados simultaneamente para a mesma conta e o saldo disponível for suficiente para ambos, o sistema deve aceitar os dois. O saldo resultante deve ser exatamente igual ao saldo anterior menos a soma dos dois débitos, e deve ser maior ou igual a zero. |

---

### US-03 — Consultar saldo

**Como** operador do sistema,  
**quero** consultar o saldo atual de uma conta,  
**para que** eu possa verificar o valor disponível.

**Critérios de aceitação:**

| # | Cenário | Critério (EARS) |
|---|---------|-----------------|
| CA-03.1 | Saldo atual | Quando uma consulta de saldo for solicitada para uma conta existente, o sistema deve retornar o saldo atual da conta em BRL com duas casas decimais. |
| CA-03.2 | Saldo não alterado | Quando uma consulta de saldo for realizada, o saldo da conta não deve ser modificado. |
| CA-03.3 | Consistência pós-lançamento | Quando um lançamento for registrado com sucesso e o saldo for consultado imediatamente após, o saldo retornado deve refletir esse lançamento. |
| CA-03.4 | Conta inexistente | Se a conta informada não existir na carga inicial, o sistema deve retornar mensagem de erro indicando que a conta não foi encontrada. |

---

### US-04 — Consultar extrato

**Como** operador do sistema,  
**quero** consultar o extrato de uma conta, com possibilidade de filtrar por período e navegar pelos resultados,  
**para que** eu possa verificar o histórico de lançamentos e a rastreabilidade das operações.

**Critérios de aceitação:**

| # | Cenário | Critério (EARS) |
|---|---------|-----------------|
| CA-04.1 | Extrato com lançamentos | Quando uma consulta de extrato for solicitada para uma conta com lançamentos, o sistema deve retornar lista contendo, para cada lançamento: identificador único, tipo, valor em BRL, descrição (quando presente) e data/hora de registro em ISO 8601 UTC com precisão de milissegundos. |
| CA-04.2 | Ordenação padrão | Quando o extrato for retornado, os lançamentos devem estar ordenados pela data/hora de registro, do mais antigo para o mais recente. Em empate de timestamp, a ordenação deve usar o identificador único em ordem crescente. |
| CA-04.3 | Extrato vazio | Quando uma consulta de extrato for solicitada para uma conta sem lançamentos, o sistema deve retornar lista vazia sem erro. |
| CA-04.4 | Paginação — padrão | Quando a consulta for feita sem informar tamanho de página, o sistema deve retornar no máximo 20 lançamentos por página e indicar o total de lançamentos disponíveis e se há páginas adicionais. |
| CA-04.5 | Paginação — tamanho explícito válido | Quando o tamanho de página informado for um inteiro entre 1 e 100, o sistema deve retornar no máximo esse número de lançamentos por página. |
| CA-04.6 | Paginação — tamanho superior a 100 | Se o tamanho de página informado for superior a 100, o sistema deve rejeitar a consulta e retornar mensagem de erro indicando que o limite máximo é 100 lançamentos por página. |
| CA-04.7 | Paginação — tamanho zero ou negativo | Se o tamanho de página informado for zero ou negativo, o sistema deve rejeitar a consulta e retornar mensagem de erro indicando que o tamanho de página deve ser maior que zero. |
| CA-04.8 | Paginação — número de página inválido | Se o número de página informado for zero, negativo ou não inteiro, o sistema deve rejeitar a consulta e retornar mensagem de erro indicando o valor inválido. |
| CA-04.9 | Filtro por período — válido | Quando o filtro de período for informado com data/hora de início e fim em ISO 8601 UTC, o sistema deve retornar apenas os lançamentos cujo timestamp de registro esteja dentro do intervalo fechado [início, fim]. Lançamentos com timestamp igual ao início ou ao fim devem ser incluídos. |
| CA-04.10 | Filtro por período — data fim anterior à data início | Se a data/hora de fim informada for anterior à data/hora de início, o sistema deve rejeitar a consulta e retornar mensagem de erro indicando o intervalo inválido. |
| CA-04.11 | Filtro por período — início igual ao fim | Quando data/hora de início e fim forem iguais, o sistema deve retornar apenas os lançamentos cujo timestamp de registro seja exatamente igual a esse valor. |
| CA-04.12 | Filtro por período — sem resultados | Quando o filtro de período for informado e nenhum lançamento existir no intervalo, o sistema deve retornar lista vazia sem erro. |
| CA-04.13 | Conta inexistente | Se a conta informada não existir na carga inicial, o sistema deve retornar mensagem de erro indicando que a conta não foi encontrada. |
| CA-04.14 | Não alteração de dados | Quando uma consulta de extrato for realizada, nenhum dado da conta ou dos lançamentos deve ser modificado. |

---

## 7. Requisitos Não Funcionais de Negócio

> Estes requisitos descrevem qualidades observáveis do serviço sob perspectiva de negócio. Não prescrevem soluções técnicas — isso será tratado na etapa de design.

### RNF-01 — Consistência dos Lançamentos

Em qualquer momento, o saldo de uma conta deve ser igual à soma de todos os créditos registrados menos a soma de todos os débitos registrados para aquela conta, somado ao saldo inicial definido na carga de dados. Não deve haver divergência entre o saldo consultável e o histórico de lançamentos no extrato.

### RNF-02 — Rastreabilidade

Cada lançamento registrado deve ser permanente e imutável após o registro. Nenhuma operação do serviço deve alterar ou excluir lançamentos já registrados. Para lançamentos rejeitados, o serviço deve preservar o motivo da rejeição de forma consultável.

### RNF-03 — Atomicidade das Operações

O registro de um lançamento e a atualização do saldo devem ser observados externamente como inseparáveis: após uma operação bem-sucedida, qualquer consulta posterior ao saldo deve refletir esse lançamento. Não deve ser possível observar um estado em que o lançamento exista sem atualização de saldo correspondente, ou vice-versa.

### RNF-04 — Auditabilidade

O serviço deve preservar, para cada lançamento processado (aceito ou rejeitado), as seguintes informações: identificador único (quando aceito), tipo da operação, valor solicitado, data/hora de registro em ISO 8601 UTC com precisão de milissegundos, resultado do processamento (aceito ou rejeitado) e, em caso de rejeição, o motivo.

### RNF-05 — Segurança de Concorrência

O serviço deve garantir que operações concorrentes sobre a mesma conta não produzam saldo negativo nem resultem em lançamentos duplicados. O comportamento deve ser determinístico e verificável pelos critérios CA-02.15 e CA-02.16.

---

## 8. Glossário

| Termo | Definição |
|-------|-----------|
| **Conta** | Entidade fictícia que representa uma conta bancária neste projeto educacional. Identificada por um código único, possui saldo e histórico de lançamentos. Definida por carga inicial de dados. |
| **Carga inicial** | Conjunto de contas fictícias predefinidas com seus saldos iniciais, carregadas antes do início da operação do serviço. O serviço não cria nem remove contas. |
| **Lançamento** | Registro de uma movimentação financeira em uma conta, podendo ser de crédito (entrada) ou débito (saída). Imutável após o registro. |
| **Crédito** | Tipo de lançamento que aumenta o saldo de uma conta. |
| **Débito** | Tipo de lançamento que diminui o saldo de uma conta. |
| **Saldo** | Valor atual disponível em uma conta, em BRL com duas casas decimais. Calculado como: saldo inicial + soma dos créditos − soma dos débitos registrados. Nunca negativo neste projeto. |
| **Extrato** | Lista paginada e ordenada dos lançamentos registrados em uma conta, consultável com filtro opcional por período. |
| **Descrição** | Campo textual opcional de um lançamento. Quando informado, deve ter entre 1 e 200 caracteres após remoção de espaços nas extremidades (*trim*). |
| **Trim** | Operação de remoção de espaços em branco no início e no fim de uma cadeia de texto, sem alterar o conteúdo interno. Aplicada à descrição antes da validação de tamanho. |
| **Identificador único** | Código que identifica de forma inequívoca cada lançamento aceito pelo serviço. Atribuído pelo serviço no momento do registro. |
| **Identificador de requisição** | Código fornecido pelo solicitante para identificar uma solicitação específica, utilizado para garantir idempotência e evitar registros duplicados. |
| **Idempotência** | Propriedade que garante que enviar a mesma solicitação mais de uma vez produz o mesmo resultado da primeira vez, sem efeitos colaterais adicionais. |
| **Paginação** | Mecanismo de divisão do extrato em páginas de tamanho fixo. Tamanho padrão: 20. Tamanho máximo: 100. A resposta indica o total de registros disponíveis e a existência de páginas adicionais. |
| **Filtro de período** | Parâmetro opcional da consulta de extrato. Define um intervalo fechado [início, fim] em UTC. O serviço filtra pelo timestamp de registro que atribuiu ao lançamento. |
| **Rastreabilidade** | Capacidade de acompanhar e verificar o histórico completo de operações realizadas em uma conta. |
| **Consistência** | Propriedade que garante que o saldo sempre reflete fielmente o histórico de lançamentos, inclusive sob operações concorrentes. |
| **Atomicidade** | Propriedade observável que garante que registro do lançamento e atualização do saldo são sempre percebidos juntos. |
| **Concorrência** | Situação em que duas ou mais solicitações de lançamento para a mesma conta chegam ao serviço simultaneamente ou em sobreposição de tempo. |
| **ISO 8601 UTC** | Formato de data/hora adotado neste projeto. Exemplo: `2026-10-14T17:30:00.123Z`. O sufixo `Z` indica UTC. |
| **BRL** | Real brasileiro (R$). Moeda única deste projeto. Valores com no máximo duas casas decimais. |
| **Mainframe** | Computador de grande porte utilizado por instituições financeiras para processar grandes volumes de transações. Representa o sistema legado sendo modernizado. |
| **Modernização** | Processo de migrar funcionalidades de sistemas legados para arquiteturas e tecnologias mais recentes, preservando o comportamento de negócio original. |
