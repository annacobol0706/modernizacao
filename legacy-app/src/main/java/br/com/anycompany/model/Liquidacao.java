package br.com.anycompany.model;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "Liquidacao")
public class Liquidacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "LojistaId")
    private Lojista lojista;

    @Temporal(TemporalType.DATE)
    private Date dataReferencia;

    private Integer qtdTransacoes;
    private BigDecimal valorBruto;
    private BigDecimal valorTaxa;
    private BigDecimal valorLiquido;
    private String arquivo;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dataCriacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Lojista getLojista() { return lojista; }
    public void setLojista(Lojista lojista) { this.lojista = lojista; }
    public Date getDataReferencia() { return dataReferencia; }
    public void setDataReferencia(Date dataReferencia) { this.dataReferencia = dataReferencia; }
    public Integer getQtdTransacoes() { return qtdTransacoes; }
    public void setQtdTransacoes(Integer qtdTransacoes) { this.qtdTransacoes = qtdTransacoes; }
    public BigDecimal getValorBruto() { return valorBruto; }
    public void setValorBruto(BigDecimal valorBruto) { this.valorBruto = valorBruto; }
    public BigDecimal getValorTaxa() { return valorTaxa; }
    public void setValorTaxa(BigDecimal valorTaxa) { this.valorTaxa = valorTaxa; }
    public BigDecimal getValorLiquido() { return valorLiquido; }
    public void setValorLiquido(BigDecimal valorLiquido) { this.valorLiquido = valorLiquido; }
    public String getArquivo() { return arquivo; }
    public void setArquivo(String arquivo) { this.arquivo = arquivo; }
    public Date getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(Date dataCriacao) { this.dataCriacao = dataCriacao; }
}
