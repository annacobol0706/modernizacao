package br.com.anycompany.web;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.anycompany.dao.AnaliseFraudeDao;
import br.com.anycompany.model.Transacao;
import br.com.anycompany.model.Usuario;
import br.com.anycompany.service.PagamentoException;
import br.com.anycompany.service.PagamentoService;

@Controller
public class RevisaoController {

    @Autowired
    private AnaliseFraudeDao analiseFraudeDao;

    @Autowired
    private PagamentoService pagamentoService;

    @RequestMapping("/revisao")
    public String fila(Model model) {
        model.addAttribute("pendentes", analiseFraudeDao.listarPendentesRevisao());
        return "revisao";
    }

    @RequestMapping(value = "/revisao/{id}/aprovar", method = RequestMethod.POST)
    public String aprovar(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Usuario u = (Usuario) session.getAttribute(LoginInterceptor.USUARIO_SESSAO);
        try {
            Transacao t = pagamentoService.aprovarRevisao(id, u.getLogin());
            ra.addFlashAttribute("mensagem", "Transacao " + id + " enviada para a adquirente: " + t.getStatus()
                    + " (" + t.getCodigoRetorno() + " " + t.getMensagemRetorno() + ")");
        } catch (PagamentoException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/revisao";
    }

    @RequestMapping(value = "/revisao/{id}/negar", method = RequestMethod.POST)
    public String negar(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Usuario u = (Usuario) session.getAttribute(LoginInterceptor.USUARIO_SESSAO);
        try {
            pagamentoService.negarRevisao(id, u.getLogin());
            ra.addFlashAttribute("mensagem", "Transacao " + id + " negada");
        } catch (PagamentoException e) {
            ra.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/revisao";
    }
}
