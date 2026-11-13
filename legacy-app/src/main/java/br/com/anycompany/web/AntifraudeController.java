package br.com.anycompany.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.anycompany.dao.FraudeJdbcDao;
import br.com.anycompany.dao.ListaBloqueioDao;
import br.com.anycompany.service.BackofficeService;
import br.com.anycompany.util.CartaoUtil;

@Controller
public class AntifraudeController {

    @Autowired
    private ListaBloqueioDao listaBloqueioDao;

    @Autowired
    private FraudeJdbcDao fraudeJdbcDao;

    @Autowired
    private BackofficeService backofficeService;

    @RequestMapping("/antifraude")
    public String regras(Model model) {
        model.addAttribute("bloqueios", listaBloqueioDao.listar());
        model.addAttribute("bins", fraudeJdbcDao.listarBinsRisco());
        return "antifraude";
    }

    @RequestMapping(value = "/antifraude/bloqueio", method = RequestMethod.POST)
    public String incluir(@RequestParam String numeroCartao, @RequestParam String motivo, RedirectAttributes ra) {
        if (!CartaoUtil.validarLuhn(numeroCartao.trim())) {
            ra.addFlashAttribute("erro", "Numero de cartao invalido");
        } else {
            backofficeService.incluirBloqueio(numeroCartao, motivo);
            ra.addFlashAttribute("mensagem", "Cartao incluido na lista de bloqueio");
        }
        return "redirect:/antifraude";
    }

    @RequestMapping(value = "/antifraude/bloqueio/{id}/remover", method = RequestMethod.POST)
    public String remover(@PathVariable Integer id, RedirectAttributes ra) {
        backofficeService.removerBloqueio(id);
        ra.addFlashAttribute("mensagem", "Cartao removido da lista de bloqueio");
        return "redirect:/antifraude";
    }
}
