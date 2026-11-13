package br.com.anycompany.ws;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.anycompany.service.PagamentoException;
import br.com.anycompany.service.PagamentoService;

/**
 * API SOAP para lojistas. WSDL em /anycompany/services/PagamentoService?wsdl
 *
 * Manual de integracao v2.3 (2015). Nao alterar a assinatura sem avisar os lojistas!
 */
@WebService(name = "PagamentoService", serviceName = "PagamentoService",
        targetNamespace = "http://ws.anycompany.com.br/pagamento/v1")
public class PagamentoWebService {

    private static final Logger log = Logger.getLogger(PagamentoWebService.class);

    @Autowired
    private PagamentoService pagamentoService;

    @WebMethod
    @WebResult(name = "resposta")
    public RespostaTransacao autorizar(@WebParam(name = "requisicao") RequisicaoAutorizacao req) {
        try {
            return RespostaTransacao.de(pagamentoService.autorizar(req.apiKey, req.pedidoId, req.numeroCartao,
                    req.nomePortador, req.validade, req.cvv, req.valor, req.moeda, req.capturaAutomatica));
        } catch (PagamentoException e) {
            return RespostaTransacao.erro(e.getCodigo(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("Erro inesperado na autorizacao", e);
            return RespostaTransacao.erro("99", "Erro interno");
        }
    }

    @WebMethod
    @WebResult(name = "resposta")
    public RespostaTransacao capturar(@WebParam(name = "requisicao") RequisicaoOperacao req) {
        try {
            return RespostaTransacao.de(pagamentoService.capturar(req.apiKey, req.transacaoId, req.valor));
        } catch (PagamentoException e) {
            return RespostaTransacao.erro(e.getCodigo(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("Erro inesperado na captura", e);
            return RespostaTransacao.erro("99", "Erro interno");
        }
    }

    @WebMethod
    @WebResult(name = "resposta")
    public RespostaTransacao cancelar(@WebParam(name = "requisicao") RequisicaoOperacao req) {
        try {
            return RespostaTransacao.de(pagamentoService.cancelar(req.apiKey, req.transacaoId));
        } catch (PagamentoException e) {
            return RespostaTransacao.erro(e.getCodigo(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("Erro inesperado no cancelamento", e);
            return RespostaTransacao.erro("99", "Erro interno");
        }
    }

    @WebMethod
    @WebResult(name = "resposta")
    public RespostaTransacao consultar(@WebParam(name = "requisicao") RequisicaoOperacao req) {
        try {
            return RespostaTransacao.de(pagamentoService.consultar(req.apiKey, req.transacaoId));
        } catch (PagamentoException e) {
            return RespostaTransacao.erro(e.getCodigo(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("Erro inesperado na consulta", e);
            return RespostaTransacao.erro("99", "Erro interno");
        }
    }
}
