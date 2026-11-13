# Operação (runbook)

*Mantido pelo time de sistemas. Última revisão: agosto de 2024.*

## Contatos

| Assunto | Quem |
|---|---|
| Aplicação | Plantão do time de sistemas (celular do plantão) |
| VMs, rede, links, backup | Infraestrutura |
| Banco de dados | DBA terceirizado (abrir chamado no portal do fornecedor) |
| Link MPLS e adquirente | Central de atendimento da adquirente |

## Deploy

**Frequência:** a cada 2 meses, aproximadamente.
**Janela:** domingo, das 1h às 3h. Avisar os lojistas por e-mail com 48 horas de antecedência.

1. O tech lead gera o WAR na máquina dele: `mvn clean package -DskipTests`.
2. Copiar o WAR para o jump01 e de lá para a app01 (`scp`).
3. Na app01:
   ```bash
   sudo systemctl stop tomcat
   cp /opt/tomcat/webapps/anycompany.war /opt/backup/anycompany-$(date +%Y%m%d).war
   rm -rf /opt/tomcat/webapps/anycompany
   cp /tmp/anycompany.war /opt/tomcat/webapps/
   sudo systemctl start tomcat
   ```
4. Esperar `Server startup` no `catalina.out` (cerca de 1 minuto).
5. Teste de fumaça: entrar no backoffice e fazer uma transação de R$ 1,00 com o cartão corporativo da diretoria. Estornar em seguida.
6. **Rollback:** repetir o passo 3 com o WAR guardado em `/opt/backup`.

A app02 não recebe deploy (ver INC-2025-021).

## Configuração

As configurações ficam no `anycompany.properties`, dentro do WAR. Mudar qualquer valor (URL da adquirente, tamanho do pool, horário do batch) exige gerar um WAR novo e fazer deploy.

Em emergência, já foi feito assim: editar `/opt/tomcat/webapps/anycompany/WEB-INF/classes/anycompany.properties` direto na app01 e reiniciar o Tomcat. A mudança se perde no próximo deploy.

## Batch de liquidação

- Roda todo dia às 2h, dentro do próprio Tomcat.
- **Não reinicie o Tomcat entre 1h50 e 2h30.**
- Às 3h, o script `/opt/scripts/copia_liquidacao.sh` (cron da app01) copia os CSVs de `/opt/tomcat/liquidacao` para o sftp01.
- Se o financeiro reclamar que falta arquivo: conferir no `anycompany.log` se aparece `Liquidacao finalizada` e rodar pelo backoffice se for preciso.

## Monitoramento

O Zabbix (mon01) acompanha:

| Item | Alerta |
|---|---|
| Ping das VMs | Sem resposta por 2 minutos |
| CPU da app01 | Acima de 90% por 5 minutos |
| Disco da app01 e da db01 | Acima de 85% |
| Porta 8080 da app01 | Fechada |

Não existe monitoramento de tempo de resposta, taxa de erro ou fila de revisão. Na prática, os primeiros a avisar quando algo está errado são os lojistas, pelo suporte.

Logs da app01:

- `/opt/tomcat/logs/anycompany.log`: log da aplicação (rotação de 10 arquivos de 50 MB).
- `/opt/tomcat/logs/catalina.out`: log do Tomcat. Não tem rotação. Apagar à mão quando o disco encher.

## O que fazer quando...

| Sintoma | Ação |
|---|---|
| Backoffice lento ou travado | Conferir CPU da app01 no Zabbix. Se estiver normal, reiniciar o Tomcat |
| Lojistas reclamando de lentidão | Ligar para a adquirente e perguntar se há instabilidade. Se não houver, reiniciar o Tomcat |
| Muitas transações PENDENTE | Conferir o link MPLS com a infraestrutura. Abrir chamado na adquirente |
| Lojista reclamando de cobrança duplicada | Seguir o procedimento do suporte no manual do backoffice |
| Arquivo de liquidação faltando | Rodar a liquidação pelo backoffice |
| Disco da app01 cheio | Apagar `catalina.out` antigo |
| Tomcat não sobe depois do deploy | Rollback |

## Manutenção

- **Índices:** o DBA roda a reindexação aos domingos às 4h. Nesse horário as consultas do backoffice ficam lentas.
- **Backup:** a infraestrutura confere o e-mail de sucesso toda segunda-feira.
- **Certificado TLS do lb01:** renovado à mão uma vez por ano. Venceu em março de 2023 e a API ficou fora por 3 horas.
- **Senha do `sa`:** conhecida pelo time de sistemas e pelo DBA. Última troca em 2019.

## Proibido

- Ligar a app02 ou colocá-la no lb01 sem aprovação da diretoria (INC-2025-021).
- Mudar o horário do batch de liquidação.
- Rodar a liquidação pelo backoffice mais de uma vez no mesmo dia.
