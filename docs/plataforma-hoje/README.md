# A plataforma hoje

Esta é a documentação que o time atual mantém sobre o gateway da AnyCompany em produção. Como em todo sistema legado, parte dela está desatualizada ou incompleta. Confira o que está escrito contra o código e contra o comportamento real do sistema (o [simulador](../simulador.md) ajuda nisso).

| Documento | Conteúdo |
|---|---|
| [01 - Negócio e números](01-negocio-e-numeros.md) | O que a empresa faz, volumes, SLAs, custos e prazos |
| [02 - Quem usa e como](02-quem-usa.md) | Lojistas, analistas, financeiro, suporte e a rotina de cada um |
| [03 - Infraestrutura](03-infraestrutura.md) | Datacenter, VMs no VMware, rede, banco e backup |
| [04 - Manual de integração do lojista](04-manual-integracao-lojista.md) | O documento que os lojistas usam para integrar (v2.3) |
| [05 - Manual do backoffice](05-manual-backoffice.md) | Telas e procedimentos dos times internos |
| [06 - Operação](06-operacao.md) | Deploy, batch, monitoramento e o que fazer quando algo falha |
| [07 - Incidentes](07-incidentes.md) | Registros dos incidentes mais relevantes dos últimos 12 meses |

O ambiente que você roda com `docker compose` reproduz a produção em pequena escala. A correspondência entre os containers e as VMs está em [03 - Infraestrutura](03-infraestrutura.md#ambiente-local-x-producao).
