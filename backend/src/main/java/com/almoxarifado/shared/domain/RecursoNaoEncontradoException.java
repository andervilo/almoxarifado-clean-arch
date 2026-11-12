package com.almoxarifado.shared.domain;

/**
 * Recurso solicitado não existe (ou não pertence à organização do usuário).
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
