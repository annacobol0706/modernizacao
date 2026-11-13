USE AnyCompany;
GO

-- =============================================================
-- Autor:     jsouza (DBA)
-- Criacao:   2013-08-14
-- Descricao: Calcula a parte do score de fraude que depende de historico.
--            O restante das regras fica no AntifraudeService (Java).
-- Alteracoes:
--   2014-02-03 jsouza - regra de BIN de risco
--   2015-11-20 mlima  - regra de valor acima da media do cartao
-- =============================================================
CREATE PROCEDURE dbo.sp_CalcularScoreFraude
    @NumeroCartao VARCHAR(19),
    @Valor        DECIMAL(18,2),
    @LojistaId    INT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @Score INT = 0;
    DECLARE @Motivos VARCHAR(1000) = '';
    DECLARE @Qtd INT;
    DECLARE @PontosBin INT;
    DECLARE @QtdHistorico INT;
    DECLARE @ValorTotal DECIMAL(18,2);

    -- Velocidade: transacoes do mesmo cartao nos ultimos 10 minutos
    SELECT @Qtd = COUNT(*)
      FROM dbo.Transacao WITH (NOLOCK)
     WHERE NumeroCartao = @NumeroCartao
       AND DataCriacao >= DATEADD(MINUTE, -10, GETDATE());

    IF @Qtd > 5
    BEGIN
        SET @Score = @Score + 50;
        SET @Motivos = @Motivos + 'VELOCIDADE_CARTAO;';
    END

    -- BIN de risco (tabela mantida pelo time de risco)
    SELECT @PontosBin = Pontuacao
      FROM dbo.BinRisco
     WHERE Bin = LEFT(@NumeroCartao, 6);

    IF @PontosBin IS NOT NULL
    BEGIN
        SET @Score = @Score + @PontosBin;
        SET @Motivos = @Motivos + 'BIN_RISCO;';
    END

    -- Valor muito acima da media historica do cartao
    SELECT @QtdHistorico = QtdTransacoes, @ValorTotal = ValorTotal
      FROM dbo.EstatisticaCartao
     WHERE NumeroCartao = @NumeroCartao;

    IF @QtdHistorico >= 3 AND @Valor > (@ValorTotal / @QtdHistorico) * 3
    BEGIN
        SET @Score = @Score + 20;
        SET @Motivos = @Motivos + 'VALOR_ACIMA_MEDIA_CARTAO;';
    END

    IF LEN(@Motivos) > 0
        SET @Motivos = LEFT(@Motivos, LEN(@Motivos) - 1);

    SELECT @Score AS Score, @Motivos AS Motivos;
END
GO

-- =============================================================
-- Autor:     jsouza (DBA)
-- Criacao:   2015-11-20
-- Descricao: Mantem a tabela EstatisticaCartao usada pela sp_CalcularScoreFraude
-- =============================================================
CREATE TRIGGER dbo.trg_Transacao_Estatistica
ON dbo.Transacao
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    MERGE dbo.EstatisticaCartao AS alvo
    USING (
        SELECT NumeroCartao, COUNT(*) AS Qtd, SUM(Valor) AS Total, MAX(DataCriacao) AS Ultima
          FROM inserted
         GROUP BY NumeroCartao
    ) AS origem
    ON alvo.NumeroCartao = origem.NumeroCartao
    WHEN MATCHED THEN
        UPDATE SET QtdTransacoes   = alvo.QtdTransacoes + origem.Qtd,
                   ValorTotal      = alvo.ValorTotal + origem.Total,
                   UltimaTransacao = origem.Ultima
    WHEN NOT MATCHED THEN
        INSERT (NumeroCartao, QtdTransacoes, ValorTotal, UltimaTransacao)
        VALUES (origem.NumeroCartao, origem.Qtd, origem.Total, origem.Ultima);
END
GO
