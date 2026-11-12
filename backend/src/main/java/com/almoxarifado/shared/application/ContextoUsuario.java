package com.almoxarifado.shared.application;

import java.util.UUID;

public interface ContextoUsuario {
    UUID organizacaoId();
    String usuarioId();
    boolean possuiPerfil(String perfil);
}
