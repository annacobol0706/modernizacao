"""Leitura do banco: API keys dos lojistas e relatorio de sintomas."""
import pymssql

from . import config


def conectar():
    if not config.DB_SENHA:
        raise SystemExit("Defina MSSQL_SA_PASSWORD (o compose ja passa o valor do .env).")
    return pymssql.connect(
        server=config.DB_HOST, port=config.DB_PORTA, user="sa", password=config.DB_SENHA,
        database="AnyCompany", login_timeout=10, timeout=120, autocommit=True,
    )


def api_keys():
    with conectar() as con:
        cur = con.cursor()
        cur.execute("SELECT Cnpj, ApiKey FROM dbo.Lojista WHERE Ativo = 1")
        return {cnpj: chave for cnpj, chave in cur.fetchall()}


def _brl(valor):
    texto = "{:,.2f}".format(float(valor or 0))
    return "R$ " + texto.replace(",", "X").replace(".", ",").replace("X", ".")


CONSULTAS = [
    (
        "Pedidos aprovados mais de uma vez (mesmo lojista e mesmo pedidoId)",
        """
        SELECT COUNT(*), ISNULL(SUM(Extra), 0)
          FROM (SELECT LojistaId, PedidoId, SUM(Valor) - MIN(Valor) AS Extra
                  FROM dbo.Transacao
                 WHERE Status IN ('AUTORIZADA', 'CAPTURADA', 'LIQUIDADA')
                 GROUP BY LojistaId, PedidoId
                HAVING COUNT(*) > 1) d
        """,
        lambda qtd, extra: "%d pedidos, %s aprovados a mais" % (qtd, _brl(extra)),
    ),
    (
        "Transacoes PENDENTE ha mais de 5 minutos",
        """
        SELECT COUNT(*), ISNULL(SUM(Valor), 0) FROM dbo.Transacao
         WHERE Status = 'PENDENTE' AND DataCriacao < DATEADD(MINUTE, -5, GETDATE())
        """,
        lambda qtd, valor: "%d transacoes, %s sem status final" % (qtd, _brl(valor)),
    ),
    (
        "Fila de revisao manual",
        """
        SELECT COUNT(*), ISNULL(DATEDIFF(MINUTE, MIN(DataCriacao), GETDATE()), 0)
          FROM dbo.Transacao WHERE Status = 'EM_REVISAO'
        """,
        lambda qtd, minutos: "%d transacoes, a mais antiga espera ha %d min" % (qtd, minutos),
    ),
    (
        "Autorizadas ha mais de 1 hora e nunca capturadas",
        """
        SELECT COUNT(*), ISNULL(SUM(Valor), 0) FROM dbo.Transacao
         WHERE Status = 'AUTORIZADA' AND DataCriacao < DATEADD(HOUR, -1, GETDATE())
        """,
        lambda qtd, valor: "%d transacoes, %s" % (qtd, _brl(valor)),
    ),
    (
        "Liquidacoes cujo liquido nao bate com bruto - taxa",
        """
        SELECT COUNT(*), ISNULL(SUM(ABS(ValorBruto - ValorTaxa - ValorLiquido)), 0)
          FROM dbo.Liquidacao WHERE ValorBruto - ValorTaxa <> ValorLiquido
        """,
        lambda qtd, dif: "%d liquidacoes, diferenca total de %s" % (qtd, _brl(dif)),
    ),
    (
        "Liquidacoes com mais transacoes declaradas do que vinculadas",
        """
        SELECT COUNT(*), ISNULL(SUM(l.ValorBruto), 0)
          FROM dbo.Liquidacao l
         WHERE l.QtdTransacoes > (SELECT COUNT(*) FROM dbo.Transacao t WHERE t.LiquidacaoId = l.Id)
        """,
        lambda qtd, valor: "%d liquidacoes, %s em liquidacoes suspeitas" % (qtd, _brl(valor)),
    ),
    (
        "Total liquidado comparado ao total capturado",
        """
        SELECT (SELECT ISNULL(SUM(ValorBruto), 0) FROM dbo.Liquidacao),
               (SELECT ISNULL(SUM(ValorCapturado), 0) FROM dbo.Transacao WHERE LiquidacaoId IS NOT NULL)
        """,
        lambda liquidado, capturado: "liquidado %s, capturado %s, diferenca %s"
        % (_brl(liquidado), _brl(capturado), _brl(float(liquidado) - float(capturado))),
    ),
]


def relatorio_sintomas():
    print("")
    print("=== Sintomas no banco (%s) ===" % config.DB_HOST)
    print("(leitura sem lock: pode incluir transacoes ainda em andamento)")
    with conectar() as con:
        cur = con.cursor()
        # O relatorio nao pode ficar preso atras das transacoes abertas do gateway.
        cur.execute("SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED")
        for titulo, sql, formatar in CONSULTAS:
            cur.execute(sql)
            print("- %s: %s" % (titulo, formatar(*cur.fetchone())))

        cur.execute(
            """
            SELECT TOP 6 m.value, COUNT(*)
              FROM dbo.AnaliseFraude a
             CROSS APPLY STRING_SPLIT(a.Motivos, ';') m
             WHERE a.DataCriacao >= DATEADD(HOUR, -24, GETDATE()) AND a.Decisao <> 'APROVAR'
             GROUP BY m.value ORDER BY COUNT(*) DESC
            """
        )
        motivos = cur.fetchall()
        if motivos:
            print("- Motivos mais frequentes de revisao/negacao nas ultimas 24h: "
                  + ", ".join("%s (%d)" % (m, q) for m, q in motivos))
    print("")
