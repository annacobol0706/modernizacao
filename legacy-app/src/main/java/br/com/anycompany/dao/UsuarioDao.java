package br.com.anycompany.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import br.com.anycompany.model.Usuario;

@Repository
public class UsuarioDao {

    @PersistenceContext
    private EntityManager em;

    public Usuario buscarPorLogin(String login) {
        List<Usuario> lista = em.createQuery("from Usuario u where u.login = :login", Usuario.class)
                .setParameter("login", login)
                .getResultList();
        return lista.isEmpty() ? null : lista.get(0);
    }
}
