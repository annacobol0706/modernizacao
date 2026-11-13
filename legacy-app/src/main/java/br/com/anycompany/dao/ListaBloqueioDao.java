package br.com.anycompany.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import br.com.anycompany.model.ListaBloqueio;

@Repository
public class ListaBloqueioDao {

    @PersistenceContext
    private EntityManager em;

    public boolean estaBloqueado(String numeroCartao) {
        Long qtd = em.createQuery("select count(b) from ListaBloqueio b where b.numeroCartao = :numero", Long.class)
                .setParameter("numero", numeroCartao)
                .getSingleResult();
        return qtd > 0;
    }

    public List<ListaBloqueio> listar() {
        return em.createQuery("from ListaBloqueio b order by b.id desc", ListaBloqueio.class).getResultList();
    }

    public void incluir(ListaBloqueio b) {
        em.persist(b);
    }

    public void remover(Integer id) {
        ListaBloqueio b = em.find(ListaBloqueio.class, id);
        if (b != null) {
            em.remove(b);
        }
    }
}
