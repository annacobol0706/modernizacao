package br.com.anycompany.model;

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
@Table(name = "AnaliseFraude")
public class AnaliseFraude {

    public static final String APROVAR = "APROVAR";
    public static final String REVISAR = "REVISAR";
    public static final String NEGAR = "NEGAR";
    public static final String APROVADA_MANUAL = "APROVADA_MANUAL";
    public static final String NEGADA_MANUAL = "NEGADA_MANUAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "TransacaoId")
    private Transacao transacao;

    private Integer score;
    private String decisao;
    private String motivos;
    private String revisor;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dataRevisao;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dataCriacao;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Transacao getTransacao() { return transacao; }
    public void setTransacao(Transacao transacao) { this.transacao = transacao; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getDecisao() { return decisao; }
    public void setDecisao(String decisao) { this.decisao = decisao; }
    public String getMotivos() { return motivos; }
    public void setMotivos(String motivos) { this.motivos = motivos; }
    public String getRevisor() { return revisor; }
    public void setRevisor(String revisor) { this.revisor = revisor; }
    public Date getDataRevisao() { return dataRevisao; }
    public void setDataRevisao(Date dataRevisao) { this.dataRevisao = dataRevisao; }
    public Date getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(Date dataCriacao) { this.dataCriacao = dataCriacao; }
}
