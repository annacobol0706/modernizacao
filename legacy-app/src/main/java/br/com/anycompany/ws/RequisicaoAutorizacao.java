package br.com.anycompany.ws;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RequisicaoAutorizacao")
public class RequisicaoAutorizacao {

    @XmlElement(required = true)
    public String apiKey;
    public String pedidoId;
    @XmlElement(required = true)
    public String numeroCartao;
    public String nomePortador;
    @XmlElement(required = true)
    public String validade;
    public String cvv;
    @XmlElement(required = true)
    public BigDecimal valor;
    public String moeda;
    public boolean capturaAutomatica;
}
