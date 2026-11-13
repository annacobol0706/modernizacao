package br.com.anycompany.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.anycompany.model.Usuario;
import br.com.anycompany.service.BackofficeService;

@Controller
public class LoginController {

    @Autowired
    private BackofficeService backofficeService;

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String form() {
        return "login";
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String entrar(@RequestParam String login, @RequestParam String senha, HttpServletRequest request,
            Model model) {
        Usuario u = backofficeService.login(login, senha);
        if (u == null) {
            model.addAttribute("erro", "Usuario ou senha invalidos");
            return "login";
        }
        HttpSession anterior = request.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        request.getSession(true).setAttribute(LoginInterceptor.USUARIO_SESSAO, u);
        return "redirect:/transacoes";
    }

    @RequestMapping("/logout")
    public String sair(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @RequestMapping("/")
    public String inicio() {
        return "redirect:/transacoes";
    }
}
