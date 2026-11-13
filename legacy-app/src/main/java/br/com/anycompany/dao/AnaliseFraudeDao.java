package br.com.anycompany.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import br.com.anycompany.model.AnaliseFraude;
import br.com.anycompany.model.Transacao;

@Repository
public class AnaliseFraudeDao {

    @PersistenceContext
    private EntityManager em;

    public void salvar(AnaliseFraude a) {
        em.persist(a);
    }

    public AnaliseFraude buscarPorTransacao(Long transacaoId) {
        List<AnaliseFraude> lista = em.createQuery("from AnaliseFraude a where a.transacao.id = :id order by a.id desc",
                AnaliseFraude.class)
                .setParameter("id", transacaoId)
                .getResultList();
        return lista.isEmpty() ? null : lista.get(0);
    }

    public List<AnaliseFraude> listarPendentesRevisao() {
        return em.createQuery("from AnaliseFraude a where a.transacao.status = :status and a.decisao = :decisao"
                + " order by a.id", AnaliseFraude.class)
                .setParameter("status", Transacao.EM_REVISAO)
                .setParameter("decisao", AnaliseFraude.REVISAR)
                .getResultList();
    }
}
