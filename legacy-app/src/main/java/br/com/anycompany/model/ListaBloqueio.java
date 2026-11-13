package br.com.anycompany.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "ListaBloqueio")
public class ListaBloqueio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String numeroCartao;
    private String motivo;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dataInclusao;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNumeroCartao() { return numeroCartao; }
    public void setNumeroCartao(String numeroCartao) { this.numeroCartao = numeroCartao; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Date getDataInclusao() { return dataInclusao; }
    public void setDataInclusao(Date dataInclusao) { this.dataInclusao = dataInclusao; }
}
