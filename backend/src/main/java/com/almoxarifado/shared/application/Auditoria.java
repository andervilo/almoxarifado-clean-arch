package com.almoxarifado.shared.application;

import java.util.UUID;

public interface Auditoria {
    void registrar(UUID organizacaoId, String usuarioId, String acao, String recurso);
}
