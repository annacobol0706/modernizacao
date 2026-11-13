# Manual de Integração AnyCompany - versão 2.3

*Documento entregue aos lojistas. Última revisão: março de 2015.*

## 1. Visão geral

A API da AnyCompany é um web service SOAP 1.1 (document/literal). Ela permite autorizar, capturar, cancelar e consultar transações de cartão de crédito.

| Ambiente | Endereço |
|---|---|
| Produção | `https://api.anycompany.example/anycompany/services/PagamentoService` |
| Homologação | `https://hml-api.anycompany.example/anycompany/services/PagamentoService` |
| Ambiente local do laboratório | `http://localhost:8080/anycompany/services/PagamentoService` |

O WSDL fica no mesmo endereço, com `?wsdl` no final. Os domínios `.example` são fictícios.

## 2. Autenticação

Toda requisição leva a `apiKey` do lojista no corpo da mensagem. A chave é enviada pelo comercial por e-mail no credenciamento. Guarde-a em local seguro.

## 3. Operações

### 3.1 autorizar

Reserva o valor no cartão do cliente. Com `capturaAutomatica = true`, a transação já é capturada em seguida.

| Campo | Obrigatório | Descrição |
|---|---|---|
| apiKey | sim | Chave do lojista |
| pedidoId | recomendado | Identificador do pedido no sistema do lojista (até 50 caracteres) |
| numeroCartao | sim | 13 a 19 dígitos, sem espaços |
| nomePortador | não | Nome impresso no cartão |
| validade | sim | `MM/AA` |
| cvv | sim | 3 ou 4 dígitos |
| valor | sim | Valor em reais, com ponto decimal (`150.00`) |
| moeda | não | Apenas `BRL` |
| capturaAutomatica | não | `true` ou `false` (padrão `false`) |

Exemplo:

```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:pag="http://ws.anycompany.com.br/pagamento/v1">
   <soapenv:Body>
      <pag:autorizar>
         <requisicao>
            <apiKey>SUA_API_KEY</apiKey>
            <pedidoId>PED-12345</pedidoId>
            <numeroCartao>4111111111111111</numeroCartao>
            <nomePortador>MARIA SILVA</nomePortador>
            <validade>12/30</validade>
            <cvv>123</cvv>
            <valor>150.00</valor>
            <moeda>BRL</moeda>
            <capturaAutomatica>false</capturaAutomatica>
         </requisicao>
      </pag:autorizar>
   </soapenv:Body>
</soapenv:Envelope>
```

### 3.2 capturar

Confirma uma transação `AUTORIZADA`. Informe `valor` para capturar menos do que foi autorizado (captura parcial). Sem `valor`, captura o total.

| Campo | Obrigatório |
|---|---|
| apiKey | sim |
| transacaoId | sim |
| valor | não |

**Autorizações não capturadas em 7 dias são canceladas automaticamente.**

### 3.3 cancelar

| Status atual | Resultado |
|---|---|
| `AUTORIZADA` | `CANCELADA` (a reserva é liberada no cartão) |
| `CAPTURADA` | `ESTORNADA` (o valor volta para o cliente) |
| `EM_REVISAO` | `CANCELADA` |
| `LIQUIDADA` | Não é possível pela API. Fale com o financeiro |

Campos: `apiKey` e `transacaoId`.

### 3.4 consultar

Retorna a situação atual da transação. Campos: `apiKey` e `transacaoId`.

## 4. Resposta

Todas as operações devolvem o mesmo bloco `resposta`:

| Campo | Descrição |
|---|---|
| transacaoId | Número da transação na AnyCompany. Guarde-o: é a única forma de capturar, cancelar ou consultar |
| pedidoId | O mesmo enviado pelo lojista |
| status | Ver seção 5 |
| codigoRetorno | Ver seção 6 |
| mensagem | Texto do retorno |
| codigoAutorizacao, nsu | Dados da adquirente, quando aprovada |
| valor, valorCapturado | Valores da transação |
| scoreFraude | Pontuação do antifraude (0 a 100) |
| dataCriacao | Data e hora da autorização |

## 5. Status

| Status | Significado |
|---|---|
| `AUTORIZADA` | Aprovada, aguardando captura |
| `CAPTURADA` | Capturada. Será liquidada no dia seguinte |
| `NEGADA` | Recusada pelo emissor ou pelo antifraude |
| `EM_REVISAO` | Em análise pela equipe de risco. Consulte novamente mais tarde |
| `PENDENTE` | Processamento ainda não concluído. Consulte novamente mais tarde |
| `CANCELADA` | Cancelada antes da captura |
| `ESTORNADA` | Cancelada depois da captura |
| `LIQUIDADA` | Valor repassado ao lojista |
| `ERRO` | A requisição não foi processada. Veja o código de retorno |

## 6. Códigos de retorno

| Código | Significado |
|---|---|
| 00 | Sucesso |
| 05 | Não autorizada pelo emissor |
| 12 | Validade em formato inválido |
| 13 | Valor inválido |
| 14 | Cartão inválido |
| 51 | Saldo ou limite insuficiente |
| 54 | Cartão vencido |
| 56 | Transação não encontrada |
| 57 | Operação não permitida no status atual |
| 96 | Falha de comunicação com a adquirente. Consulte a transação mais tarde |
| 99 | Erro interno |
| AK | API key inválida |
| FR | Negada pelo antifraude |
| FR01 | Enviada para revisão manual |

## 7. Boas práticas

1. **Timeout:** configure sua integração para esperar até 10 segundos pela resposta.
2. **Reenvio:** em caso de timeout ou erro de comunicação, reenvie a mesma requisição com o mesmo `pedidoId`. A AnyCompany identifica requisições repetidas.
3. **Revisão manual:** transações `EM_REVISAO` costumam ser decididas em até 30 minutos em horário comercial. Consulte a cada 30 segundos.
4. **Captura:** capture somente depois de confirmar o pedido (estoque, reserva etc.).
5. **Dados do cartão:** não guarde número do cartão nem CVV no seu sistema.

## 8. Arquivo de liquidação

Todo dia, até as 8h, a AnyCompany disponibiliza no SFTP um arquivo CSV por lojista com as transações liquidadas, no formato:

```
liquidacao;transacao;pedido;cartao;valor;taxa;liquido
...
TOTAL;<quantidade>;;;<bruto>;<taxa>;<liquido>
```

O valor é depositado na conta do lojista em D+1.

## 9. Cartões de teste (homologação)

| Cartão | Resultado |
|---|---|
| 4111111111111111 | Aprovado |
| 5555555555554444 | Aprovado |
| 4000000000000002 | Não autorizado (05) |
| Valor terminado em ,51 | Saldo insuficiente (51) |

## 10. Histórico de versões

| Versão | Data | Mudança |
|---|---|---|
| 1.0 | 2012-06 | Primeira versão: autorizar e cancelar |
| 2.0 | 2014-02 | Antifraude, status `EM_REVISAO`, operação consultar |
| 2.2 | 2014-10 | Captura posterior |
| 2.3 | 2015-03 | Captura parcial |
