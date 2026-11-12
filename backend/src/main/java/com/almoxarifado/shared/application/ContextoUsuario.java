package com.almoxarifado.shared.application;

import java.util.UUID;

public interface ContextoUsuario {
    UUID organizacaoId();
    String usuarioId();
    boolean possuiPerfil(String perfil);
    /** null representa o escopo irrestrito do tenant; conjunto vazio nega todos os almoxarifados. */
    default java.util.Set<UUID> almoxarifadosPermitidos() { return null; }
    default boolean permiteAlmoxarifado(UUID id) {
        var escopo=almoxarifadosPermitidos();
        return escopo==null || (id!=null && escopo.contains(id));
    }
    default void exigirAlmoxarifado(UUID id) {
        if(!permiteAlmoxarifado(id)) throw new org.springframework.security.access.AccessDeniedException("Almoxarifado fora do escopo autorizado");
    }
}
