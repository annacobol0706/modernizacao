package br.com.anycompany.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.anycompany.dao.AnaliseFraudeDao;
import br.com.anycompany.dao.LojistaDao;
import br.com.anycompany.dao.TransacaoDao;
import br.com.anycompany.model.Transacao;

@Controller
public class TransacaoController {

    @Autowired
    private TransacaoDao transacaoDao;

    @Autowired
    private LojistaDao lojistaDao;

    @Autowired
    private AnaliseFraudeDao analiseFraudeDao;

    @RequestMapping("/transacoes")
    public String listar(@RequestParam(required = false) String status,
            @RequestParam(required = false) Integer lojista, Model model) {
        model.addAttribute("transacoes", transacaoDao.listar(status, lojista, 200));
        model.addAttribute("totais", transacaoDao.contarPorStatus());
        model.addAttribute("lojistas", lojistaDao.listarTodos());
        model.addAttribute("statusSelecionado", status);
        model.addAttribute("lojistaSelecionado", lojista);
        return "transacoes";
    }

    @RequestMapping("/transacoes/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        Transacao t = transacaoDao.buscar(id);
        if (t == null) {
            return "redirect:/transacoes";
        }
        model.addAttribute("t", t);
        model.addAttribute("analise", analiseFraudeDao.buscarPorTransacao(id));
        return "transacao";
    }
}
