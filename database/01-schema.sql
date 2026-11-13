-- =============================================================
-- AnyCompany - schema principal
-- Todas as areas (pagamento, antifraude, liquidacao, backoffice)
-- compartilham o mesmo banco e o mesmo schema dbo.
-- =============================================================
CREATE DATABASE AnyCompany;
GO

USE AnyCompany;
GO

CREATE TABLE dbo.Lojista (
    Id               INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Lojista PRIMARY KEY,
    Nome             VARCHAR(100)  NOT NULL,
    Cnpj             VARCHAR(14)   NOT NULL,
    ApiKey           VARCHAR(64)   NOT NULL,
    TaxaMdr          DECIMAL(5,2)  NOT NULL,
    LimiteTransacao  DECIMAL(18,2) NOT NULL,
    Ativo            BIT           NOT NULL CONSTRAINT DF_Lojista_Ativo DEFAULT 1,
    DataCadastro     DATETIME      NOT NULL CONSTRAINT DF_Lojista_DataCadastro DEFAULT GETDATE()
);
GO

CREATE TABLE dbo.Liquidacao (
    Id               BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Liquidacao PRIMARY KEY,
    LojistaId        INT           NOT NULL CONSTRAINT FK_Liquidacao_Lojista REFERENCES dbo.Lojista (Id),
    DataReferencia   DATE          NOT NULL,
    QtdTransacoes    INT           NOT NULL,
    ValorBruto       DECIMAL(18,2) NOT NULL,
    ValorTaxa        DECIMAL(18,2) NOT NULL,
    ValorLiquido     DECIMAL(18,2) NOT NULL,
    Arquivo          VARCHAR(260)  NULL,
    DataCriacao      DATETIME      NOT NULL CONSTRAINT DF_Liquidacao_DataCriacao DEFAULT GETDATE()
);
GO

CREATE TABLE dbo.Transacao (
    Id                 BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Transacao PRIMARY KEY,
    LojistaId          INT           NOT NULL CONSTRAINT FK_Transacao_Lojista REFERENCES dbo.Lojista (Id),
    PedidoId           VARCHAR(50)   NULL,
    NumeroCartao       VARCHAR(19)   NOT NULL,
    NomePortador       VARCHAR(100)  NULL,
    Validade           CHAR(5)       NOT NULL,
    Cvv                VARCHAR(4)    NULL,
    Bandeira           VARCHAR(20)   NULL,
    Valor              DECIMAL(18,2) NOT NULL,
    ValorCapturado     DECIMAL(18,2) NULL,
    Moeda              CHAR(3)       NOT NULL CONSTRAINT DF_Transacao_Moeda DEFAULT 'BRL',
    Status             VARCHAR(20)   NOT NULL,
    CodigoRetorno      VARCHAR(10)   NULL,
    MensagemRetorno    VARCHAR(200)  NULL,
    CodigoAutorizacao  VARCHAR(10)   NULL,
    Nsu                VARCHAR(20)   NULL,
    ScoreFraude        INT           NULL,
    LiquidacaoId       BIGINT        NULL CONSTRAINT FK_Transacao_Liquidacao REFERENCES dbo.Liquidacao (Id),
    DataCriacao        DATETIME      NOT NULL CONSTRAINT DF_Transacao_DataCriacao DEFAULT GETDATE(),
    DataAtualizacao    DATETIME      NULL
);
GO

CREATE INDEX IX_Transacao_Cartao_Data ON dbo.Transacao (NumeroCartao, DataCriacao);
CREATE INDEX IX_Transacao_Status_Lojista ON dbo.Transacao (Status, LojistaId);
GO

CREATE TABLE dbo.AnaliseFraude (
    Id           BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT PK_AnaliseFraude PRIMARY KEY,
    TransacaoId  BIGINT        NOT NULL CONSTRAINT FK_AnaliseFraude_Transacao REFERENCES dbo.Transacao (Id),
    Score        INT           NOT NULL,
    Decisao      VARCHAR(20)   NOT NULL,
    Motivos      VARCHAR(1000) NULL,
    Revisor      VARCHAR(50)   NULL,
    DataRevisao  DATETIME      NULL,
    DataCriacao  DATETIME      NOT NULL CONSTRAINT DF_AnaliseFraude_DataCriacao DEFAULT GETDATE()
);
GO

CREATE TABLE dbo.ListaBloqueio (
    Id            INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_ListaBloqueio PRIMARY KEY,
    NumeroCartao  VARCHAR(19)  NOT NULL,
    Motivo        VARCHAR(200) NULL,
    DataInclusao  DATETIME     NOT NULL CONSTRAINT DF_ListaBloqueio_Data DEFAULT GETDATE()
);
GO

CREATE TABLE dbo.BinRisco (
    Bin        CHAR(6)      NOT NULL CONSTRAINT PK_BinRisco PRIMARY KEY,
    Pais       CHAR(2)      NOT NULL,
    Pontuacao  INT          NOT NULL,
    Descricao  VARCHAR(200) NULL
);
GO

-- Mantida pela trigger trg_Transacao_Estatistica
CREATE TABLE dbo.EstatisticaCartao (
    NumeroCartao     VARCHAR(19)   NOT NULL CONSTRAINT PK_EstatisticaCartao PRIMARY KEY,
    QtdTransacoes    INT           NOT NULL,
    ValorTotal       DECIMAL(18,2) NOT NULL,
    UltimaTransacao  DATETIME      NOT NULL
);
GO

CREATE TABLE dbo.Usuario (
    Id        INT IDENTITY(1,1) NOT NULL CONSTRAINT PK_Usuario PRIMARY KEY,
    Login     VARCHAR(50)  NOT NULL CONSTRAINT UQ_Usuario_Login UNIQUE,
    SenhaMd5  CHAR(32)     NOT NULL,
    Nome      VARCHAR(100) NOT NULL,
    Perfil    VARCHAR(20)  NOT NULL
);
GO
