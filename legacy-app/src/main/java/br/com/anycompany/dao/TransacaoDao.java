package br.com.anycompany.dao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.springframework.stereotype.Repository;

import br.com.anycompany.model.Transacao;

@Repository
public class TransacaoDao {

    @PersistenceContext
    private EntityManager em;

    public void salvar(Transacao t) {
        em.persist(t);
    }

    public Transacao atualizar(Transacao t) {
        return em.merge(t);
    }

    public Transacao buscar(Long id) {
        return em.find(Transacao.class, id);
    }

    public List<Transacao> listar(String status, Integer lojistaId, int max) {
        StringBuilder jpql = new StringBuilder("from Transacao t where 1 = 1");
        if (status != null && !status.isEmpty()) {
            jpql.append(" and t.status = :status");
        }
        if (lojistaId != null) {
            jpql.append(" and t.lojista.id = :lojistaId");
        }
        jpql.append(" order by t.id desc");
        TypedQuery<Transacao> q = em.createQuery(jpql.toString(), Transacao.class);
        if (status != null && !status.isEmpty()) {
            q.setParameter("status", status);
        }
        if (lojistaId != null) {
            q.setParameter("lojistaId", lojistaId);
        }
        return q.setMaxResults(max).getResultList();
    }

    public List<Transacao> listarCapturadasNaoLiquidadas(Integer lojistaId) {
        return em.createQuery("from Transacao t where t.lojista.id = :lojistaId and t.status = :status"
                + " and t.liquidacaoId is null order by t.id", Transacao.class)
                .setParameter("lojistaId", lojistaId)
                .setParameter("status", Transacao.CAPTURADA)
                .getResultList();
    }

    public List<Transacao> listarPorLiquidacao(Long liquidacaoId) {
        return em.createQuery("from Transacao t where t.liquidacaoId = :id order by t.id", Transacao.class)
                .setParameter("id", liquidacaoId)
                .getResultList();
    }

    public Map<String, Long> contarPorStatus() {
        List<Object[]> linhas = em.createQuery("select t.status, count(t) from Transacao t group by t.status order by t.status",
                Object[].class).getResultList();
        Map<String, Long> mapa = new LinkedHashMap<String, Long>();
        for (Object[] linha : linhas) {
            mapa.put((String) linha[0], (Long) linha[1]);
        }
        return mapa;
    }
}
