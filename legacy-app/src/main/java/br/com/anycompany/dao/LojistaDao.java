package br.com.anycompany.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import br.com.anycompany.model.Lojista;

@Repository
public class LojistaDao {

    @PersistenceContext
    private EntityManager em;

    public Lojista buscar(Integer id) {
        return em.find(Lojista.class, id);
    }

    public Lojista buscarPorApiKey(String apiKey) {
        List<Lojista> lista = em.createQuery("from Lojista l where l.apiKey = :apiKey and l.ativo = :ativo", Lojista.class)
                .setParameter("apiKey", apiKey)
                .setParameter("ativo", Boolean.TRUE)
                .getResultList();
        return lista.isEmpty() ? null : lista.get(0);
    }

    public List<Lojista> listarTodos() {
        return em.createQuery("from Lojista l order by l.id", Lojista.class).getResultList();
    }
}
