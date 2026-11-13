# Desafio: modernizar o gateway da AnyCompany

## Contexto

O gateway da AnyCompany processa pagamentos de 3.200 lojistas e roda em VMs VMware num datacenter de colocation em São Paulo. Na última Black Friday aconteceu o seguinte:

- O gateway ficou lento e, em alguns momentos, o backoffice também parou de responder.
- Lojistas reclamaram de clientes cobrados duas vezes.
- O financeiro encontrou diferenças de centavos entre o arquivo de liquidação e o extrato.
- Meses depois, o time tentou subir uma segunda instância do Tomcat para aguentar o Carnaval e precisou desfazer a mudança às pressas.

E vários prazos estão chegando ao mesmo tempo (detalhes em [negócio e números](plataforma-hoje/01-negocio-e-numeros.md)):

- a renovação do VMware vai mais que triplicar de preço em 6 meses;
- a auditoria PCI DSS é em 6 meses;
- o contrato do datacenter vence em 14 meses e não será renovado;
- o comercial projeta uma Black Friday com quase o dobro do pico do ano passado.

A diretoria decidiu sair do datacenter e ir para a AWS. Você foi chamada para liderar a migração e a modernização.

## Restrições

- **Os lojistas não mudam de integração tão cedo.** O contrato SOAP atual precisa continuar funcionando por pelo menos seis meses depois que a nova plataforma entrar no ar.
- **Não existe janela de manutenção longa.** A migração precisa ser incremental. Uma reescrita completa com troca num único dia ("big bang") não é aceitável.
- **Os dados históricos precisam ser preservados**, incluindo transações e liquidações (1,8 TB hoje).
- **A adquirente continua a mesma.** Hoje a conexão é um link MPLS com o datacenter. A nova plataforma precisa de uma conexão equivalente.
- **Você escolhe a stack de destino:** Java moderno (por exemplo Java 21 com Spring Boot 3 ou Quarkus) ou .NET 8. Justifique a escolha.
- **Tudo precisa rodar localmente em containers.** Se quiser, você também pode publicar numa conta AWS própria (veja a seção "Rodando na AWS" do README).

## Material de apoio

- [A plataforma hoje](plataforma-hoje/README.md): números do negócio, quem usa, infraestrutura, manuais, runbook e incidentes. Escrito pelo time atual. Nem tudo o que está lá é verdade.
- [Simulador](simulador.md): reproduz o uso real e os incidentes na sua máquina.

## Etapas e entregáveis

### 1. Descoberta

Rode o sistema, leia o código, o banco e a documentação. Use o backoffice, a API e o simulador.

Entregue:

- uma lista de problemas que você encontrou, com a evidência de cada um (arquivo, linha, teste ou cenário do simulador que reproduz) e o impacto para o negócio;
- para cada incidente em [incidentes](plataforma-hoje/07-incidentes.md), a causa raiz que você encontrou, comparada com a que foi registrada;
- os pontos em que a documentação não bate com o comportamento do sistema;
- um mapa dos domínios do monolito e das dependências entre eles (código e tabelas);
- testes de caracterização que registram o comportamento atual da API antes de você mexer em qualquer coisa;
- uma linha de base medida com o simulador: latência, timeouts e sintomas no `dia-normal` e no `black-friday`.

### 2. Arquitetura alvo

Entregue:

- um diagrama da arquitetura alvo na AWS;
- um ADR (Architecture Decision Record) para cada decisão importante, com contexto, opções consideradas, decisão e consequências;
- uma estimativa de custo mensal da arquitetura alvo para o volume atual e para o pico projetado, comparada com o custo atual.

Decisões que você vai precisar tomar, entre outras:

| Tema | Pergunta |
|---|---|
| Caminho de migração | Primeiro tirar as VMs do datacenter como estão (rehost) e modernizar depois na AWS, ou modernizar já durante a saída? O prazo de 14 meses permite o quê? |
| Estratégia por módulo | Rehost, replatform ou refactor? Vale a mesma resposta para todos os módulos? |
| Fronteiras | Quais serviços existem? Por qual você começa e por quê? |
| Runtime | Java ou .NET? Qual framework? |
| Computação | AWS Lambda, Amazon ECS com Fargate ou Amazon EKS, para cada serviço? Como latência, cold start, custo e carga constante pesam nessa escolha? |
| Dados | Manter SQL Server (Amazon RDS for SQL Server)? Migrar para Aurora PostgreSQL, com ou sem Babelfish? DynamoDB para alguma parte? Um banco por serviço? Como mover 1,8 TB sem parar a operação? |
| Lógica no banco | O que fazer com a procedure e a trigger? |
| Integração | Chamadas síncronas (REST/gRPC) ou eventos (Amazon SQS, Amazon EventBridge)? Onde cada um faz sentido? |
| Contrato SOAP | Como manter o contrato atual enquanto os serviços novos entram? |
| Conectividade | Como a AWS fala com a adquirente durante e depois da migração (Site-to-Site VPN, Direct Connect)? E com o datacenter durante a transição? |
| Batch | Como a liquidação deve rodar quando houver várias instâncias? |
| Segurança | Como tratar dados de cartão, segredos e autenticação dos lojistas e dos analistas? O que muda no escopo PCI? |
| Operação | Logs, métricas, tracing, health checks, alarmes e deploy. |
| Resiliência | Backup, recuperação, RPO e RTO. O que acontece se uma zona de disponibilidade cair? |

### 3. Plano de migração

Entregue um plano em fases que caiba nos 14 meses:

- a ordem de migração e de extração dos serviços;
- como o tráfego é desviado do datacenter e do monolito para a AWS e para os serviços novos;
- como os dados migram e como ficam consistentes durante a transição;
- como você faz rollback de cada fase;
- o que precisa estar pronto antes da próxima Black Friday.

### 4. Implementação

Implemente pelo menos um serviço extraído do monolito, funcionando junto com o legado:

- roda localmente em container, ou como Lambda (com AWS SAM CLI ou LocalStack, por exemplo);
- tem testes automatizados;
- o contrato SOAP continua respondendo igual para o lojista;
- os testes de caracterização da etapa 1 continuam passando;
- os cenários do simulador mostram a melhora em relação à linha de base.

### 5. Apresentação

Apresente em até 20 minutos: os problemas mais graves, a arquitetura alvo, as decisões principais, o plano de migração e o que você faria nas próximas fases.

## Ferramentas que podem ajudar

- **Saída do VMware:** AWS Transform for VMware (descoberta, planejamento de ondas e rehost das VMs para EC2), AWS Application Migration Service.
- **Upgrade de Java:** Amazon Q Developer (transformação de código Java 8 para 17/21), AWS Transform custom, OpenRewrite (receitas de migração para Spring Boot 3 e `javax` para `jakarta`).
- **Banco:** AWS Schema Conversion Tool, AWS Database Migration Service, Babelfish Compass (avalia a compatibilidade de código T-SQL com o Babelfish).
- **Custo:** AWS Pricing Calculator.
- **Testes:** Testcontainers, WireMock, SoapUI ou Postman, e o simulador deste repositório.
- **Execução local de serviços AWS:** AWS SAM CLI, LocalStack.

## Como vamos trabalhar

Traga suas dúvidas e decisões para as sessões de mentoria. Não existe uma única resposta certa. O que conta é você entender os trade-offs de cada caminho e conseguir defender suas escolhas.
