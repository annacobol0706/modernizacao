package br.com.anycompany.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.anycompany.dao.ListaBloqueioDao;
import br.com.anycompany.dao.UsuarioDao;
import br.com.anycompany.model.ListaBloqueio;
import br.com.anycompany.model.Usuario;
import br.com.anycompany.util.Md5Util;

@Service
public class BackofficeService {

    @Autowired
    private UsuarioDao usuarioDao;

    @Autowired
    private ListaBloqueioDao listaBloqueioDao;

    @Transactional(readOnly = true)
    public Usuario login(String login, String senha) {
        if (login == null || senha == null) {
            return null;
        }
        Usuario u = usuarioDao.buscarPorLogin(login);
        if (u != null && u.getSenhaMd5().equalsIgnoreCase(Md5Util.md5(senha))) {
            return u;
        }
        return null;
    }

    @Transactional
    public void incluirBloqueio(String numeroCartao, String motivo) {
        ListaBloqueio b = new ListaBloqueio();
        b.setNumeroCartao(numeroCartao.trim());
        b.setMotivo(motivo);
        b.setDataInclusao(new Date());
        listaBloqueioDao.incluir(b);
    }

    @Transactional
    public void removerBloqueio(Integer id) {
        listaBloqueioDao.remover(id);
    }
}
