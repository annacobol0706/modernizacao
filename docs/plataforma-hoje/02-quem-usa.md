# Quem usa e como

## Lojistas

Os lojistas chamam a API SOAP descrita no [manual de integração](04-manual-integracao-lojista.md). Na prática existem três tipos de integração, e cada uma se comporta de um jeito diferente quando o gateway demora:

| Tipo | Quem usa | Fluxo | Timeout | Quando dá timeout |
|---|---|---|---|---|
| PDV via integrador TEF ("PDV Fácil") | Varejo físico | Autoriza com captura automática | 5 s | Reenvia até 2 vezes, com o mesmo pedido |
| Plugin de loja virtual | E-commerce | Autoriza e captura quando o pedido é separado no estoque, de segundos a minutos depois. Cancela cerca de 4% antes de capturar | 8 s | Reenvia 1 vez, com o mesmo pedido |
| Integração própria | Turismo e serviços | Autoriza e captura depois de confirmar a reserva. Se a transação cair em revisão, consulta o status a cada 30 s | 20 s | Não reenvia. Mostra erro para o cliente |

Os lojistas só têm acesso à API. Para qualquer outra coisa, ligam ou mandam e-mail para o suporte.

## Analistas de risco (3 pessoas, das 8h às 22h)

- Abrem a **fila de revisão** no backoffice a cada 15 minutos e decidem cada transação olhando o valor, o lojista e o histórico do cartão.
- Toda segunda-feira recebem da adquirente uma planilha com os chargebacks da semana e cadastram os cartões na **lista de bloqueio**, um por um.
- Quando querem mudar a pontuação de um BIN de risco, abrem um chamado para o DBA, porque não existe tela para isso.
- Os três usam o mesmo usuário `admin` do backoffice.
- Às 8h fazem um mutirão para limpar a fila que acumulou durante a noite. Desde maio de 2024 essa fila noturna ficou bem maior (ver PRB-2024-009 em [incidentes](07-incidentes.md)).

## Financeiro (2 pessoas)

- Às 8h conferem os arquivos de liquidação do dia anterior contra o extrato da adquirente.
- Diferenças de centavos são lançadas na planilha `ajustes_liquidacao.xlsx` e acertadas com o lojista no fim do mês.
- Estorno de transação já liquidada é feito à mão: o financeiro debita o valor do próximo repasse do lojista.
- Se o arquivo de um lojista não foi gerado, pedem ao suporte para clicar em **Executar liquidação agora** no backoffice.

## Suporte (4 pessoas, das 8h às 20h)

- Atendem lojistas por telefone e e-mail.
- Consultam transações no backoffice pelo número da transação. Não existe busca por pedido ou por cartão. Quando o lojista só tem o número do pedido, o suporte pede uma consulta ao time de sistemas.
- Casos de "cliente cobrado duas vezes": orientam o lojista a cancelar uma das transações pela API. Se já foi liquidada, encaminham para o financeiro.
- Transações `PENDENTE` são escaladas para o time de sistemas.

## Time de sistemas (4 desenvolvedores Java e 1 tech lead)

- Mantêm o código e fazem os deploys (ver [operação](06-operacao.md)).
- Plantão por telefone, em escala semanal.
- O tech lead é o único que conhece o sistema inteiro. Os outros três entraram há menos de dois anos.

## DBA terceirizado (40 horas por mês)

- Mantém a procedure de antifraude e a trigger de estatística.
- Faz as alterações de BIN de risco pedidas pelos analistas.
- Corrige status de transações `PENDENTE` com `UPDATE` direto no banco, depois que o financeiro confirma com a adquirente.

## Infraestrutura (2 pessoas)

- Cuidam do VMware, das VMs, do firewall, dos links e do backup.
- Monitoram o Zabbix em horário comercial.

## Um dia típico

| Horário | O que acontece |
|---|---|
| 00h a 06h | Pouco movimento. A fila de revisão acumula, porque não há analistas |
| 02h | Batch de liquidação (segundo o runbook) |
| 03h | Script copia os arquivos de liquidação para o SFTP |
| 08h | Financeiro confere a liquidação. Analistas fazem o mutirão da fila |
| 12h a 14h | Primeiro pico (almoço, varejo físico) |
| 19h a 21h | Pico do dia (e-commerce) |
| 22h | Último analista sai |
