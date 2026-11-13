package br.com.anycompany.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import br.com.anycompany.dao.LojistaDao;

@Controller
public class LojistaController {

    @Autowired
    private LojistaDao lojistaDao;

    @RequestMapping("/lojistas")
    public String listar(Model model) {
        model.addAttribute("lojistas", lojistaDao.listarTodos());
        return "lojistas";
    }
}
