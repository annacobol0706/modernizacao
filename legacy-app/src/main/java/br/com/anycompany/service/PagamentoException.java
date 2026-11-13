package br.com.anycompany.service;

/**
 * Erro de negocio com codigo de retorno no padrao da adquirente.
 */
public class PagamentoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String codigo;

    public PagamentoException(String codigo, String mensagem) {
        super(mensagem);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
