package br.com.anycompany.ws;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Usada em captura, cancelamento e consulta. O valor so e usado na captura parcial.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RequisicaoOperacao")
public class RequisicaoOperacao {

    @XmlElement(required = true)
    public String apiKey;
    @XmlElement(required = true)
    public Long transacaoId;
    public BigDecimal valor;
}
