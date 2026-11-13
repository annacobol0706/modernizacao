package br.com.anycompany.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import br.com.anycompany.model.Transacao;

/**
 * Cliente da adquirente (protocolo XML sobre HTTP, versao 1 do manual de integracao).
 */
@Component
public class AdquirenteClient {

    private static final Logger log = Logger.getLogger(AdquirenteClient.class);

    @Value("${adquirente.url}")
    private String urlBase;

    public static class Resposta {
        public String codigo;
        public String mensagem;
        public String codigoAutorizacao;
        public String nsu;

        public boolean isAprovada() {
            return "00".equals(codigo);
        }
    }

    public Resposta autorizar(Transacao t) throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<requisicaoAdquirente>"
                + "<estabelecimento>" + t.getLojista().getCnpj() + "</estabelecimento>"
                + "<pedido>" + t.getId() + "</pedido>"
                + "<numeroCartao>" + t.getNumeroCartao() + "</numeroCartao>"
                + "<validade>" + t.getValidade() + "</validade>"
                + "<cvv>" + t.getCvv() + "</cvv>"
                + "<valor>" + t.getValor().setScale(2).toPlainString() + "</valor>"
                + "<moeda>" + t.getMoeda() + "</moeda>"
                + "</requisicaoAdquirente>";
        return enviar("/autorizar", xml);
    }

    public Resposta capturar(Transacao t, BigDecimal valor) throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<requisicaoCaptura>"
                + "<estabelecimento>" + t.getLojista().getCnpj() + "</estabelecimento>"
                + "<nsu>" + t.getNsu() + "</nsu>"
                + "<valor>" + valor.setScale(2).toPlainString() + "</valor>"
                + "</requisicaoCaptura>";
        return enviar("/capturar", xml);
    }

    public Resposta cancelar(Transacao t) throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<requisicaoCancelamento>"
                + "<estabelecimento>" + t.getLojista().getCnpj() + "</estabelecimento>"
                + "<nsu>" + t.getNsu() + "</nsu>"
                + "</requisicaoCancelamento>";
        return enviar("/cancelar", xml);
    }

    private Resposta enviar(String caminho, String xml) throws IOException {
        log.info("Enviando para adquirente " + caminho + ": " + xml);
        long inicio = System.currentTimeMillis();

        HttpURLConnection con = (HttpURLConnection) new URL(urlBase + caminho).openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/xml; charset=UTF-8");

        OutputStream out = con.getOutputStream();
        out.write(xml.getBytes("UTF-8"));
        out.close();

        int status = con.getResponseCode();
        if (status != 200) {
            throw new IOException("Adquirente retornou HTTP " + status);
        }

        InputStream in = con.getInputStream();
        byte[] corpo = lerTudo(in);
        in.close();

        log.info("Resposta da adquirente em " + (System.currentTimeMillis() - inicio) + "ms: " + new String(corpo, "UTF-8"));
        return parse(corpo);
    }

    private Resposta parse(byte[] corpo) throws IOException {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(corpo));
            Resposta r = new Resposta();
            r.codigo = texto(doc, "codigo");
            r.mensagem = texto(doc, "mensagem");
            r.codigoAutorizacao = texto(doc, "codigoAutorizacao");
            r.nsu = texto(doc, "nsu");
            return r;
        } catch (Exception e) {
            throw new IOException("Resposta invalida da adquirente", e);
        }
    }

    private String texto(Document doc, String tag) {
        NodeList nl = doc.getElementsByTagName(tag);
        return nl.getLength() == 0 ? null : nl.item(0).getTextContent();
    }

    private byte[] lerTudo(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
