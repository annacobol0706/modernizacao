# Manual do backoffice

Endereço em produção: `https://backoffice.anycompany.example/anycompany` (somente pela rede do escritório). No laboratório: http://localhost:8080/anycompany.

## Acesso

Use o usuário `admin`. Peça a senha ao coordenador de risco. Não existe recuperação de senha pela tela. A sessão expira depois de 60 minutos sem uso.

## Telas

### Transações

Lista as 200 transações mais recentes, com totais por status no topo. Dá para filtrar por status e por lojista. Clique no número da transação para ver o detalhe e a análise de fraude.

Não existe busca por pedido nem por cartão. Para isso, peça uma consulta ao time de sistemas.

### Revisão de fraude

Transações com score entre 50 e 79 esperando decisão.

- **Aprovar:** a transação é enviada para a adquirente na hora. O resultado aparece no topo da tela.
- **Negar:** a transação fica `NEGADA` e o lojista vê o código `FR`.

Critérios da equipe de risco:

1. Cartão com histórico de compras normais no mesmo lojista: aprovar.
2. Várias compras pequenas seguidas no mesmo cartão (`VELOCIDADE_CARTAO`): negar. É o padrão de teste de cartão roubado.
3. Valor acima do limite do lojista em lojista de turismo: ligar para o lojista antes de decidir.
4. Na dúvida, negar.

### Regras antifraude

Mostra a tabela de pontuação, a lista de bloqueio e os BINs de risco.

- **Bloquear cartão:** informe o número e o motivo. A partir daí toda transação do cartão é negada.
- **Remover bloqueio:** só com autorização do coordenador de risco.
- **BIN de risco:** não pode ser alterado pela tela. Abra um chamado para o DBA.

### Liquidação

Lista as liquidações geradas e o caminho de cada arquivo.

O botão **Executar liquidação agora** roda o batch na hora, para os casos em que o arquivo da madrugada não foi gerado. Clique uma vez só e espere a mensagem de confirmação.

### Lojistas

Lista os lojistas com CNPJ, API key, MDR e limite por transação. Alterações de cadastro são feitas pelo time de sistemas direto no banco.

## Procedimentos

### Analista de risco: rotina

1. Abrir a fila de revisão a cada 15 minutos.
2. Decidir cada transação pelos critérios acima.
3. Às segundas-feiras, cadastrar na lista de bloqueio os cartões da planilha de chargebacks da adquirente.

### Suporte: lojista diz que o cliente foi cobrado duas vezes

1. Pedir ao lojista os números das transações.
2. Conferir no backoffice se as duas estão `AUTORIZADA` ou `CAPTURADA`.
3. Orientar o lojista a cancelar uma delas pela API (operação `cancelar`).
4. Se uma delas já estiver `LIQUIDADA`, abrir chamado para o financeiro.

### Suporte: transação PENDENTE

1. Não orientar o lojista a reenviar.
2. Abrir chamado para o time de sistemas com o número da transação.
3. O financeiro confere com a adquirente no dia seguinte e o DBA corrige o status.

### Financeiro: conferência diária

1. Às 8h, baixar do SFTP os arquivos de liquidação do dia anterior.
2. Conferir o total de cada arquivo contra o extrato da adquirente.
3. Lançar diferenças na planilha `ajustes_liquidacao.xlsx`.
4. Se faltar o arquivo de algum lojista, pedir ao suporte para executar a liquidação pelo backoffice e avisar o time de sistemas.
