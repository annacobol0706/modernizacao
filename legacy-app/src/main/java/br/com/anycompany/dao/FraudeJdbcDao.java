package br.com.anycompany.dao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Acesso JDBC direto para as rotinas de fraude que ficam no banco.
 * (mais rapido que Hibernate - decisao do DBA em 2013)
 */
@Repository
public class FraudeJdbcDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Executa a procedure dbo.sp_CalcularScoreFraude.
     * Retorna um mapa com as colunas Score (int) e Motivos (varchar separado por ';').
     */
    public Map<String, Object> calcularScore(String numeroCartao, BigDecimal valor, Integer lojistaId) {
        return jdbcTemplate.queryForMap("EXEC dbo.sp_CalcularScoreFraude ?, ?, ?", numeroCartao, valor, lojistaId);
    }

    public List<Map<String, Object>> listarBinsRisco() {
        return jdbcTemplate.queryForList("SELECT Bin, Pais, Pontuacao, Descricao FROM dbo.BinRisco ORDER BY Pontuacao DESC");
    }
}
