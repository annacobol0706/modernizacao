package br.com.anycompany.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.anycompany.dao.AnaliseFraudeDao;
import br.com.anycompany.dao.FraudeJdbcDao;
import br.com.anycompany.dao.ListaBloqueioDao;
import br.com.anycompany.model.AnaliseFraude;
import br.com.anycompany.model.Transacao;

/**
 * Motor de antifraude.
 *
 * Parte das regras esta aqui e parte na procedure dbo.sp_CalcularScoreFraude
 * (regras que precisam de historico ficam no banco por performance).
 *
 * @author rcosta
 * @author mlima - regra de madrugada (2015)
 */
@Service
public class AntifraudeService {

    private static final Logger log = Logger.getLogger(AntifraudeService.class);

    @Autowired
    private ListaBloqueioDao listaBloqueioDao;

    @Autowired
    private FraudeJdbcDao fraudeJdbcDao;

    @Autowired
    private AnaliseFraudeDao analiseFraudeDao;

    /**
     * A transacao precisa ja estar gravada (a procedure conta a propria transacao).
     */
    public AnaliseFraude analisar(Transacao t) {
        int score = 0;
        List<String> motivos = new ArrayList<String>();

        if (listaBloqueioDao.estaBloqueado(t.getNumeroCartao())) {
            score = 100;
            motivos.add("CARTAO_LISTA_BLOQUEIO");
        } else {
            if (t.getValor().compareTo(t.getLojista().getLimiteTransacao()) > 0) {
                score += 40;
                motivos.add("VALOR_ACIMA_LIMITE_LOJISTA");
            }

            int hora = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            if (hora >= 0 && hora < 6) {
                score += 15;
                motivos.add("HORARIO_MADRUGADA");
            }

            Map<String, Object> resultado = fraudeJdbcDao.calcularScore(t.getNumeroCartao(), t.getValor(),
                    t.getLojista().getId());
            score += ((Number) resultado.get("Score")).intValue();
            String motivosBanco = (String) resultado.get("Motivos");
            if (motivosBanco != null && !motivosBanco.isEmpty()) {
                motivos.addAll(Arrays.asList(motivosBanco.split(";")));
            }
        }

        if (score > 100) {
            score = 100;
        }

        String decisao;
        if (score >= 80) {
            decisao = AnaliseFraude.NEGAR;
        } else if (score >= 50) {
            decisao = AnaliseFraude.REVISAR;
        } else {
            decisao = AnaliseFraude.APROVAR;
        }

        AnaliseFraude analise = new AnaliseFraude();
        analise.setTransacao(t);
        analise.setScore(score);
        analise.setDecisao(decisao);
        analise.setMotivos(juntar(motivos));
        analise.setDataCriacao(new Date());
        analiseFraudeDao.salvar(analise);

        log.info("Analise de fraude transacao=" + t.getId() + " cartao=" + t.getNumeroCartao()
                + " valor=" + t.getValor() + " score=" + score + " decisao=" + decisao + " motivos=" + motivos);
        return analise;
    }

    private String juntar(List<String> lista) {
        StringBuilder sb = new StringBuilder();
        for (String s : lista) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(s);
        }
        return sb.toString();
    }
}
