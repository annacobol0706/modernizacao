package br.com.anycompany.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.anycompany.dao.LiquidacaoDao;
import br.com.anycompany.dao.LojistaDao;
import br.com.anycompany.dao.TransacaoDao;
import br.com.anycompany.model.Liquidacao;
import br.com.anycompany.model.Lojista;
import br.com.anycompany.model.Transacao;
import br.com.anycompany.util.CartaoUtil;

/**
 * Liquidacao financeira. Roda todo dia as 02:00 pelo Quartz (ver applicationContext.xml)
 * e tambem pode ser disparada pelo backoffice.
 *
 * Gera um arquivo CSV por lojista com as transacoes capturadas e ainda nao liquidadas.
 */
@Service
public class LiquidacaoService {

    private static final Logger log = Logger.getLogger(LiquidacaoService.class);

    @Autowired
    private LojistaDao lojistaDao;

    @Autowired
    private TransacaoDao transacaoDao;

    @Autowired
    private LiquidacaoDao liquidacaoDao;

    @Value("${liquidacao.diretorio}")
    private String diretorio;

    @Transactional
    public int executar() {
        log.info("Iniciando liquidacao");

        // 1. Monta o lote de todos os lojistas antes de gerar os arquivos
        Map<Lojista, List<Transacao>> lotes = new LinkedHashMap<Lojista, List<Transacao>>();
        for (Lojista lojista : lojistaDao.listarTodos()) {
            List<Transacao> transacoes = transacaoDao.listarCapturadasNaoLiquidadas(lojista.getId());
            if (!transacoes.isEmpty()) {
                lotes.put(lojista, transacoes);
            }
        }
        log.info("Lote montado: " + lotes.size() + " lojistas com transacoes a liquidar");

        // 2. Gera a liquidacao e o arquivo de cada lojista
        int geradas = 0;
        for (Map.Entry<Lojista, List<Transacao>> lote : lotes.entrySet()) {
            Lojista lojista = lote.getKey();
            List<Transacao> transacoes = lote.getValue();

            double bruto = 0;
            for (Transacao t : transacoes) {
                bruto += t.getValorCapturado().doubleValue();
            }
            double taxa = bruto * lojista.getTaxaMdr().doubleValue() / 100;
            double liquido = bruto - taxa;

            Liquidacao l = new Liquidacao();
            l.setLojista(lojista);
            l.setDataReferencia(new Date());
            l.setQtdTransacoes(transacoes.size());
            l.setValorBruto(new BigDecimal(bruto).setScale(2, BigDecimal.ROUND_DOWN));
            l.setValorTaxa(new BigDecimal(taxa).setScale(2, BigDecimal.ROUND_DOWN));
            l.setValorLiquido(new BigDecimal(liquido).setScale(2, BigDecimal.ROUND_DOWN));
            l.setDataCriacao(new Date());
            liquidacaoDao.salvar(l);

            String arquivo = gerarArquivo(lojista, l, transacoes);
            l.setArquivo(arquivo);

            for (Transacao t : transacoes) {
                t.setStatus(Transacao.LIQUIDADA);
                t.setLiquidacaoId(l.getId());
                t.setDataAtualizacao(new Date());
            }

            log.info("Liquidacao " + l.getId() + " lojista=" + lojista.getId() + " qtd=" + transacoes.size()
                    + " bruto=" + bruto + " taxa=" + taxa + " liquido=" + liquido);
            geradas++;
        }
        log.info("Liquidacao finalizada. Lojistas liquidados: " + geradas);
        return geradas;
    }

    private String gerarArquivo(Lojista lojista, Liquidacao l, List<Transacao> transacoes) {
        File dir = new File(diretorio);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String nome = "LIQ_" + lojista.getCnpj() + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv";
        File arquivo = new File(dir, nome);
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(arquivo));
            pw.println("liquidacao;transacao;pedido;cartao;valor;taxa;liquido");
            for (Transacao t : transacoes) {
                double valor = t.getValorCapturado().doubleValue();
                double taxa = Math.round(valor * lojista.getTaxaMdr().doubleValue()) / 100.0;
                pw.println(l.getId() + ";" + t.getId() + ";" + t.getPedidoId() + ";"
                        + CartaoUtil.mascarar(t.getNumeroCartao()) + ";" + valor + ";" + taxa + ";" + (valor - taxa));
            }
            pw.println("TOTAL;" + transacoes.size() + ";;;" + l.getValorBruto() + ";" + l.getValorTaxa() + ";"
                    + l.getValorLiquido());
            pw.close();
        } catch (IOException e) {
            log.error("Erro ao gerar arquivo de liquidacao", e);
        }
        return arquivo.getAbsolutePath();
    }
}
