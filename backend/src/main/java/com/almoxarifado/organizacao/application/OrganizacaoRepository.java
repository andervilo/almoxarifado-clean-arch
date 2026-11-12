package com.almoxarifado.organizacao.application;
import java.util.*;
import com.almoxarifado.organizacao.domain.*;
public interface OrganizacaoRepository {
    Optional<Organizacao> buscar(UUID id);
    void salvar(UUID id, Organizacao organizacao);
    List<Setor> setores(UUID organizacaoId);
    void salvarSetor(UUID organizacaoId, Setor setor);
    List<Usuario> usuarios(UUID organizacaoId);
    void salvarUsuario(UUID organizacaoId, Usuario usuario);
}
