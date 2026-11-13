package br.com.anycompany.ws;

import java.math.BigDecimal;
import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import br.com.anycompany.model.Transacao;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RespostaTransacao")
public class RespostaTransacao {

    public Long transacaoId;
    public String pedidoId;
    public String status;
    public String codigoRetorno;
    public String mensagem;
    public String codigoAutorizacao;
    public String nsu;
    public BigDecimal valor;
    public BigDecimal valorCapturado;
    public Integer scoreFraude;
    public Date dataCriacao;

    public static RespostaTransacao de(Transacao t) {
        RespostaTransacao r = new RespostaTransacao();
        r.transacaoId = t.getId();
        r.pedidoId = t.getPedidoId();
        r.status = t.getStatus();
        r.codigoRetorno = t.getCodigoRetorno();
        r.mensagem = t.getMensagemRetorno();
        r.codigoAutorizacao = t.getCodigoAutorizacao();
        r.nsu = t.getNsu();
        r.valor = t.getValor();
        r.valorCapturado = t.getValorCapturado();
        r.scoreFraude = t.getScoreFraude();
        r.dataCriacao = t.getDataCriacao();
        return r;
    }

    public static RespostaTransacao erro(String codigo, String mensagem) {
        RespostaTransacao r = new RespostaTransacao();
        r.status = "ERRO";
        r.codigoRetorno = codigo;
        r.mensagem = mensagem;
        return r;
    }
}
