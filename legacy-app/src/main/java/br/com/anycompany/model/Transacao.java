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
@Table(name = "Transacao")
public class Transacao {

    // Status possiveis (ver tambem a coluna Status no banco)
    public static final String PENDENTE = "PENDENTE";
    public static final String AUTORIZADA = "AUTORIZADA";
    public static final String NEGADA = "NEGADA";
    public static final String EM_REVISAO = "EM_REVISAO";
    public static final String CAPTURADA = "CAPTURADA";
    public static final String CANCELADA = "CANCELADA";
    public static final String ESTORNADA = "ESTORNADA";
    public static final String LIQUIDADA = "LIQUIDADA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "LojistaId")
    private Lojista lojista;

    private String pedidoId;
    private String numeroCartao;
    private String nomePortador;
    private String validade;
    private String cvv;
    private String bandeira;
    private BigDecimal valor;
    private BigDecimal valorCapturado;
    private String moeda;
    private String status;
    private String codigoRetorno;
    private String mensagemRetorno;
    private String codigoAutorizacao;
    private String nsu;
    private Integer scoreFraude;
    private Long liquidacaoId;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dataCriacao;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dataAtualizacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Lojista getLojista() { return lojista; }
    public void setLojista(Lojista lojista) { this.lojista = lojista; }
    public String getPedidoId() { return pedidoId; }
    public void setPedidoId(String pedidoId) { this.pedidoId = pedidoId; }
    public String getNumeroCartao() { return numeroCartao; }
    public void setNumeroCartao(String numeroCartao) { this.numeroCartao = numeroCartao; }
    public String getNomePortador() { return nomePortador; }
    public void setNomePortador(String nomePortador) { this.nomePortador = nomePortador; }
    public String getValidade() { return validade; }
    public void setValidade(String validade) { this.validade = validade; }
    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
    public String getBandeira() { return bandeira; }
    public void setBandeira(String bandeira) { this.bandeira = bandeira; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public BigDecimal getValorCapturado() { return valorCapturado; }
    public void setValorCapturado(BigDecimal valorCapturado) { this.valorCapturado = valorCapturado; }
    public String getMoeda() { return moeda; }
    public void setMoeda(String moeda) { this.moeda = moeda; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCodigoRetorno() { return codigoRetorno; }
    public void setCodigoRetorno(String codigoRetorno) { this.codigoRetorno = codigoRetorno; }
    public String getMensagemRetorno() { return mensagemRetorno; }
    public void setMensagemRetorno(String mensagemRetorno) { this.mensagemRetorno = mensagemRetorno; }
    public String getCodigoAutorizacao() { return codigoAutorizacao; }
    public void setCodigoAutorizacao(String codigoAutorizacao) { this.codigoAutorizacao = codigoAutorizacao; }
    public String getNsu() { return nsu; }
    public void setNsu(String nsu) { this.nsu = nsu; }
    public Integer getScoreFraude() { return scoreFraude; }
    public void setScoreFraude(Integer scoreFraude) { this.scoreFraude = scoreFraude; }
    public Long getLiquidacaoId() { return liquidacaoId; }
    public void setLiquidacaoId(Long liquidacaoId) { this.liquidacaoId = liquidacaoId; }
    public Date getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(Date dataCriacao) { this.dataCriacao = dataCriacao; }
    public Date getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(Date dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }
}
