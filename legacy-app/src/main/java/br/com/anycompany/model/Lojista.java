package br.com.anycompany.model;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "Lojista")
public class Lojista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nome;
    private String cnpj;
    private String apiKey;
    private BigDecimal taxaMdr;
    private BigDecimal limiteTransacao;
    private boolean ativo;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dataCadastro;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public BigDecimal getTaxaMdr() { return taxaMdr; }
    public void setTaxaMdr(BigDecimal taxaMdr) { this.taxaMdr = taxaMdr; }
    public BigDecimal getLimiteTransacao() { return limiteTransacao; }
    public void setLimiteTransacao(BigDecimal limiteTransacao) { this.limiteTransacao = limiteTransacao; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Date getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(Date dataCadastro) { this.dataCadastro = dataCadastro; }
}
