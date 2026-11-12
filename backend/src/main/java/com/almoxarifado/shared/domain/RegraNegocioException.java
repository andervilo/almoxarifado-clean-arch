package com.almoxarifado.shared.domain;

/**
 * Violação de regra de negócio. Carrega um código estável (ex.: {@code SALDO_INSUFICIENTE})
 * que é exposto na API para que o cliente possa tratar o erro.
 * Não depende de framework, podendo ser lançada pelo domínio.
 */
public class RegraNegocioException extends RuntimeException {

    private final String codigo;

    public RegraNegocioException(String codigo, String mensagem) {
        super(mensagem);
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Código do erro de negócio é obrigatório");
        }
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
