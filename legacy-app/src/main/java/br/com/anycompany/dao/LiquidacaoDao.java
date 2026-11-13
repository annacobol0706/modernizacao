package br.com.anycompany.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import br.com.anycompany.model.Liquidacao;

@Repository
public class LiquidacaoDao {

    @PersistenceContext
    private EntityManager em;

    public void salvar(Liquidacao l) {
        em.persist(l);
    }

    public Liquidacao buscar(Long id) {
        return em.find(Liquidacao.class, id);
    }

    public List<Liquidacao> listar() {
        return em.createQuery("from Liquidacao l order by l.id desc", Liquidacao.class)
                .setMaxResults(100)
                .getResultList();
    }
}
