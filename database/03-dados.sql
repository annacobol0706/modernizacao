USE AnyCompany;
GO

SET NOCOUNT ON;

-- ---------------- Lojistas ----------------
-- API keys geradas aleatoriamente na criacao do banco. Consulte no backoffice (menu Lojistas).
INSERT INTO dbo.Lojista (Nome, Cnpj, ApiKey, TaxaMdr, LimiteTransacao, Ativo)
VALUES ('Loja Exemplo Eletronicos', '12345678000190',
        LOWER(CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', CAST(NEWID() AS VARCHAR(36))), 2)), 2.49, 5000.00, 1);
INSERT INTO dbo.Lojista (Nome, Cnpj, ApiKey, TaxaMdr, LimiteTransacao, Ativo)
VALUES ('Padaria Pao Quente', '23456789000101',
        LOWER(CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', CAST(NEWID() AS VARCHAR(36))), 2)), 1.99, 500.00, 1);
INSERT INTO dbo.Lojista (Nome, Cnpj, ApiKey, TaxaMdr, LimiteTransacao, Ativo)
VALUES ('Viagens Ceu Azul', '34567890000112',
        LOWER(CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', CAST(NEWID() AS VARCHAR(36))), 2)), 3.20, 20000.00, 1);
INSERT INTO dbo.Lojista (Nome, Cnpj, ApiKey, TaxaMdr, LimiteTransacao, Ativo)
VALUES ('Loja Antiga Desativada', '45678901000123',
        LOWER(CONVERT(VARCHAR(64), HASHBYTES('SHA2_256', CAST(NEWID() AS VARCHAR(36))), 2)), 2.99, 1000.00, 0);
GO

-- ---------------- Antifraude ----------------
INSERT INTO dbo.BinRisco (Bin, Pais, Pontuacao, Descricao) VALUES
    ('529999', 'NG', 30, 'Emissor estrangeiro com alto indice de chargeback'),
    ('491761', 'GB', 25, 'Cartao pre-pago internacional'),
    ('650499', 'BR', 10, 'BIN com fraude recente reportada pela bandeira');

INSERT INTO dbo.ListaBloqueio (NumeroCartao, Motivo) VALUES
    ('4000000000000069', 'Chargeback por fraude confirmada'),
    ('5162920000000002', 'Cartao reportado como roubado');
GO

-- ---------------- Usuarios do backoffice ----------------
INSERT INTO dbo.Usuario (Login, SenhaMd5, Nome, Perfil)
VALUES ('admin', LOWER(CONVERT(VARCHAR(32), HASHBYTES('MD5', '$(SenhaAdmin)'), 2)), 'Administrador', 'ADMIN');
GO

-- ---------------- Historico de transacoes (ultimos 7 dias) ----------------
DECLARE @Cartoes TABLE (Pos INT IDENTITY(1,1), Numero VARCHAR(19), Bandeira VARCHAR(20), Nome VARCHAR(100));
INSERT INTO @Cartoes (Numero, Bandeira, Nome) VALUES
    ('4532015112830366', 'VISA',       'MARIA S OLIVEIRA'),
    ('5425233430109903', 'MASTERCARD', 'JOAO P SANTOS'),
    ('4012888888881881', 'VISA',       'ANA C FERREIRA'),
    ('5522400012345674', 'MASTERCARD', 'CARLOS E SOUZA'),
    ('6363681234567894', 'ELO',        'JULIANA M COSTA'),
    ('4389350000123457', 'ELO',        'PEDRO H ALMEIDA'),
    ('4111111111112226', 'VISA',       'FERNANDA R LIMA');

DECLARE @i INT = 1;
DECLARE @Lojista INT, @Cartao INT, @Limite DECIMAL(18,2), @Valor DECIMAL(18,2), @Data DATETIME;
DECLARE @Sorteio INT, @Status VARCHAR(20), @Codigo VARCHAR(10), @Msg VARCHAR(200);
DECLARE @Score INT, @Decisao VARCHAR(20), @Motivos VARCHAR(1000), @Id BIGINT;

WHILE @i <= 400
BEGIN
    SET @Lojista = 1 + ABS(CHECKSUM(NEWID())) % 3;
    SET @Cartao  = 1 + ABS(CHECKSUM(NEWID())) % 7;
    SELECT @Limite = LimiteTransacao FROM dbo.Lojista WHERE Id = @Lojista;
    SET @Valor   = ROUND(5 + (ABS(CHECKSUM(NEWID())) % 10000) / 10000.0 * (@Limite * 0.6), 2);
    SET @Data    = DATEADD(MINUTE, -(60 + ABS(CHECKSUM(NEWID())) % (7 * 24 * 60)), GETDATE());
    SET @Sorteio = ABS(CHECKSUM(NEWID())) % 100;
    SET @Score   = ABS(CHECKSUM(NEWID())) % 40;
    SET @Decisao = 'APROVAR';
    SET @Motivos = NULL;

    IF @Sorteio < 65
        SELECT @Status = 'CAPTURADA', @Codigo = '00', @Msg = 'Transacao autorizada';
    ELSE IF @Sorteio < 75
        SELECT @Status = 'AUTORIZADA', @Codigo = '00', @Msg = 'Transacao autorizada';
    ELSE IF @Sorteio < 85
        SELECT @Status = 'NEGADA', @Codigo = '05', @Msg = 'Nao autorizada pelo emissor';
    ELSE IF @Sorteio < 90
        SELECT @Status = 'NEGADA', @Codigo = 'FR', @Msg = 'Transacao negada pelo antifraude',
               @Score = 80 + ABS(CHECKSUM(NEWID())) % 21, @Decisao = 'NEGAR',
               @Motivos = 'VALOR_ACIMA_LIMITE_LOJISTA;VELOCIDADE_CARTAO', @Valor = ROUND(@Limite * 1.3, 2);
    ELSE IF @Sorteio < 95
        SELECT @Status = 'CANCELADA', @Codigo = '00', @Msg = 'Cancelamento realizado';
    ELSE IF @Sorteio < 98
        SELECT @Status = 'EM_REVISAO', @Codigo = 'FR01', @Msg = 'Transacao em analise de risco',
               @Score = 55 + ABS(CHECKSUM(NEWID())) % 20, @Decisao = 'REVISAR',
               @Motivos = 'VALOR_ACIMA_LIMITE_LOJISTA;HORARIO_MADRUGADA', @Valor = ROUND(@Limite * 1.1, 2);
    ELSE
        SELECT @Status = 'PENDENTE', @Codigo = '96', @Msg = 'Erro de comunicacao com a adquirente';

    INSERT INTO dbo.Transacao (LojistaId, PedidoId, NumeroCartao, NomePortador, Validade, Cvv, Bandeira, Valor,
                               ValorCapturado, Moeda, Status, CodigoRetorno, MensagemRetorno, CodigoAutorizacao, Nsu,
                               ScoreFraude, DataCriacao, DataAtualizacao)
    SELECT @Lojista,
           'PED-' + CAST(100000 + @i AS VARCHAR(10)),
           c.Numero,
           c.Nome,
           RIGHT('0' + CAST(1 + ABS(CHECKSUM(NEWID())) % 12 AS VARCHAR(2)), 2) + '/' + CAST(28 + ABS(CHECKSUM(NEWID())) % 5 AS VARCHAR(2)),
           RIGHT('00' + CAST(ABS(CHECKSUM(NEWID())) % 1000 AS VARCHAR(3)), 3),
           c.Bandeira,
           @Valor,
           CASE WHEN @Status = 'CAPTURADA' THEN @Valor END,
           'BRL',
           @Status,
           @Codigo,
           @Msg,
           CASE WHEN @Status IN ('CAPTURADA', 'AUTORIZADA', 'CANCELADA')
                THEN RIGHT('000000' + CAST(ABS(CHECKSUM(NEWID())) % 1000000 AS VARCHAR(6)), 6) END,
           CASE WHEN @Status IN ('CAPTURADA', 'AUTORIZADA', 'CANCELADA')
                THEN CAST(100000000 + ABS(CHECKSUM(NEWID())) % 900000000 AS VARCHAR(20)) END,
           @Score,
           @Data,
           @Data
      FROM @Cartoes c
     WHERE c.Pos = @Cartao;

    SET @Id = SCOPE_IDENTITY();

    INSERT INTO dbo.AnaliseFraude (TransacaoId, Score, Decisao, Motivos, DataCriacao)
    VALUES (@Id, @Score, @Decisao, @Motivos, @Data);

    SET @i = @i + 1;
END
GO
