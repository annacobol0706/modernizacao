package br.com.anycompany.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.anycompany.dao.LiquidacaoDao;
import br.com.anycompany.service.LiquidacaoService;

@Controller
public class LiquidacaoController {

    @Autowired
    private LiquidacaoDao liquidacaoDao;

    @Autowired
    private LiquidacaoService liquidacaoService;

    @RequestMapping("/liquidacao")
    public String listar(Model model) {
        model.addAttribute("liquidacoes", liquidacaoDao.listar());
        return "liquidacao";
    }

    @RequestMapping(value = "/liquidacao/executar", method = RequestMethod.POST)
    public String executar(RedirectAttributes ra) {
        int qtd = liquidacaoService.executar();
        ra.addFlashAttribute("mensagem", "Liquidacao executada. Lojistas liquidados: " + qtd);
        return "redirect:/liquidacao";
    }
}
