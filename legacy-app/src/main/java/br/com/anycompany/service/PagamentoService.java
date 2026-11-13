package br.com.anycompany.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.anycompany.dao.AnaliseFraudeDao;
import br.com.anycompany.dao.LojistaDao;
import br.com.anycompany.dao.TransacaoDao;
import br.com.anycompany.model.AnaliseFraude;
import br.com.anycompany.model.Lojista;
import br.com.anycompany.model.Transacao;
import br.com.anycompany.util.CartaoUtil;

/**
 * Regras de pagamento: autorizacao, captura, cancelamento e revisao manual.
 *
 * @author rcosta
 */
@Service
@Transactional
public class PagamentoService {

    private static final Logger log = Logger.getLogger(PagamentoService.class);

    @Autowired
    private LojistaDao lojistaDao;

    @Autowired
    private TransacaoDao transacaoDao;

    @Autowired
    private AnaliseFraudeDao analiseFraudeDao;

    @Autowired
    private AntifraudeService antifraudeService;

    @Autowired
    private AdquirenteClient adquirenteClient;

    public Transacao autorizar(String apiKey, String pedidoId, String numeroCartao, String nomePortador,
            String validade, String cvv, BigDecimal valor, String moeda, boolean capturaAutomatica) {

        Lojista lojista = autenticar(apiKey);

        log.info("Autorizacao recebida lojista=" + lojista.getId() + " pedido=" + pedidoId + " cartao=" + numeroCartao
                + " validade=" + validade + " valor=" + valor);

        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PagamentoException("13", "Valor invalido");
        }
        if (!CartaoUtil.validarLuhn(numeroCartao)) {
            throw new PagamentoException("14", "Cartao invalido");
        }
        if (validade == null || !validade.matches("\\d{2}/\\d{2}")) {
            throw new PagamentoException("12", "Validade invalida, use MM/AA");
        }
        if (CartaoUtil.validadeExpirada(validade)) {
            throw new PagamentoException("54", "Cartao vencido");
        }

        Transacao t = new Transacao();
        t.setLojista(lojista);
        t.setPedidoId(pedidoId);
        t.setNumeroCartao(numeroCartao);
        t.setNomePortador(nomePortador);
        t.setValidade(validade);
        t.setCvv(cvv);
        t.setBandeira(CartaoUtil.identificarBandeira(numeroCartao));
        t.setValor(valor);
        t.setMoeda(moeda == null ? "BRL" : moeda);
        t.setStatus(Transacao.PENDENTE);
        t.setDataCriacao(new Date());
        transacaoDao.salvar(t);

        AnaliseFraude analise = antifraudeService.analisar(t);
        t.setScoreFraude(analise.getScore());

        if (AnaliseFraude.NEGAR.equals(analise.getDecisao())) {
            t.setStatus(Transacao.NEGADA);
            t.setCodigoRetorno("FR");
            t.setMensagemRetorno("Transacao negada pelo antifraude");
        } else if (AnaliseFraude.REVISAR.equals(analise.getDecisao())) {
            t.setStatus(Transacao.EM_REVISAO);
            t.setCodigoRetorno("FR01");
            t.setMensagemRetorno("Transacao em analise de risco");
        } else {
            enviarParaAdquirente(t, capturaAutomatica);
        }

        t.setDataAtualizacao(new Date());
        return t;
    }

    public Transacao capturar(String apiKey, Long transacaoId, BigDecimal valor) {
        Lojista lojista = autenticar(apiKey);
        Transacao t = buscarDoLojista(lojista, transacaoId);

        if (!Transacao.AUTORIZADA.equals(t.getStatus())) {
            throw new PagamentoException("57", "Transacao nao pode ser capturada no status " + t.getStatus());
        }
        BigDecimal valorCaptura = valor == null ? t.getValor() : valor;
        if (valorCaptura.compareTo(t.getValor()) > 0) {
            throw new PagamentoException("13", "Valor de captura maior que o autorizado");
        }
        capturarNaAdquirente(t, valorCaptura);
        return t;
    }

    public Transacao cancelar(String apiKey, Long transacaoId) {
        Lojista lojista = autenticar(apiKey);
        Transacao t = buscarDoLojista(lojista, transacaoId);

        String status = t.getStatus();
        if (Transacao.EM_REVISAO.equals(status)) {
            t.setStatus(Transacao.CANCELADA);
            t.setMensagemRetorno("Cancelada pelo lojista durante revisao");
        } else if (Transacao.AUTORIZADA.equals(status) || Transacao.CAPTURADA.equals(status)) {
            AdquirenteClient.Resposta r;
            try {
                r = adquirenteClient.cancelar(t);
            } catch (IOException e) {
                log.error("Erro ao cancelar transacao " + t.getId(), e);
                throw new PagamentoException("96", "Erro de comunicacao com a adquirente");
            }
            if (!r.isAprovada()) {
                throw new PagamentoException(r.codigo, r.mensagem);
            }
            t.setStatus(Transacao.AUTORIZADA.equals(status) ? Transacao.CANCELADA : Transacao.ESTORNADA);
            t.setCodigoRetorno(r.codigo);
            t.setMensagemRetorno(r.mensagem);
        } else if (Transacao.LIQUIDADA.equals(status)) {
            throw new PagamentoException("57", "Transacao ja liquidada. Estorno deve ser solicitado ao financeiro");
        } else {
            throw new PagamentoException("57", "Transacao nao pode ser cancelada no status " + status);
        }
        t.setDataAtualizacao(new Date());
        return t;
    }

    @Transactional(readOnly = true)
    public Transacao consultar(String apiKey, Long transacaoId) {
        Lojista lojista = autenticar(apiKey);
        return buscarDoLojista(lojista, transacaoId);
    }

    /**
     * Chamado pelo backoffice quando o analista aprova uma transacao em revisao.
     */
    public Transacao aprovarRevisao(Long transacaoId, String revisor) {
        Transacao t = transacaoDao.buscar(transacaoId);
        if (t == null || !Transacao.EM_REVISAO.equals(t.getStatus())) {
            throw new PagamentoException("57", "Transacao nao esta em revisao");
        }
        AnaliseFraude analise = analiseFraudeDao.buscarPorTransacao(transacaoId);
        analise.setDecisao(AnaliseFraude.APROVADA_MANUAL);
        analise.setRevisor(revisor);
        analise.setDataRevisao(new Date());

        // TODO guardar a flag de captura automatica na transacao (chamado #4512)
        enviarParaAdquirente(t, false);
        t.setDataAtualizacao(new Date());
        return t;
    }

    public Transacao negarRevisao(Long transacaoId, String revisor) {
        Transacao t = transacaoDao.buscar(transacaoId);
        if (t == null || !Transacao.EM_REVISAO.equals(t.getStatus())) {
            throw new PagamentoException("57", "Transacao nao esta em revisao");
        }
        AnaliseFraude analise = analiseFraudeDao.buscarPorTransacao(transacaoId);
        analise.setDecisao(AnaliseFraude.NEGADA_MANUAL);
        analise.setRevisor(revisor);
        analise.setDataRevisao(new Date());

        t.setStatus(Transacao.NEGADA);
        t.setCodigoRetorno("FR");
        t.setMensagemRetorno("Transacao negada na revisao manual");
        t.setDataAtualizacao(new Date());
        return t;
    }

    private void enviarParaAdquirente(Transacao t, boolean capturaAutomatica) {
        AdquirenteClient.Resposta r;
        try {
            r = adquirenteClient.autorizar(t);
        } catch (IOException e) {
            log.error("Erro de comunicacao com a adquirente na transacao " + t.getId(), e);
            t.setCodigoRetorno("96");
            t.setMensagemRetorno("Erro de comunicacao com a adquirente");
            return;
        }

        t.setCodigoRetorno(r.codigo);
        t.setMensagemRetorno(r.mensagem);
        if (r.isAprovada()) {
            t.setStatus(Transacao.AUTORIZADA);
            t.setCodigoAutorizacao(r.codigoAutorizacao);
            t.setNsu(r.nsu);
            if (capturaAutomatica) {
                capturarNaAdquirente(t, t.getValor());
            }
        } else {
            t.setStatus(Transacao.NEGADA);
        }
    }

    private void capturarNaAdquirente(Transacao t, BigDecimal valor) {
        AdquirenteClient.Resposta r;
        try {
            r = adquirenteClient.capturar(t, valor);
        } catch (IOException e) {
            log.error("Erro ao capturar transacao " + t.getId(), e);
            throw new PagamentoException("96", "Erro de comunicacao com a adquirente");
        }
        if (!r.isAprovada()) {
            throw new PagamentoException(r.codigo, r.mensagem);
        }
        t.setStatus(Transacao.CAPTURADA);
        t.setValorCapturado(valor);
        t.setDataAtualizacao(new Date());
    }

    private Lojista autenticar(String apiKey) {
        Lojista lojista = apiKey == null ? null : lojistaDao.buscarPorApiKey(apiKey);
        if (lojista == null) {
            throw new PagamentoException("AK", "API key invalida");
        }
        return lojista;
    }

    private Transacao buscarDoLojista(Lojista lojista, Long transacaoId) {
        Transacao t = transacaoId == null ? null : transacaoDao.buscar(transacaoId);
        if (t == null || !t.getLojista().getId().equals(lojista.getId())) {
            throw new PagamentoException("56", "Transacao nao encontrada");
        }
        return t;
    }
}
