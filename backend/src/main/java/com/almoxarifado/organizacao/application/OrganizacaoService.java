package com.almoxarifado.organizacao.application;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import com.almoxarifado.organizacao.domain.*;
import com.almoxarifado.organizacao.events.OrganizacaoAlterada;
import com.almoxarifado.shared.application.*;
import com.almoxarifado.shared.domain.*;
@Service
@Transactional(readOnly=true)
@PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','REQUISITANTE','GESTOR')")
public class OrganizacaoService {
    private final OrganizacaoRepository repository;
    private final ContextoUsuario contexto;
    private final Auditoria auditoria;
    private final PublicadorEventos eventos;
    public OrganizacaoService(OrganizacaoRepository repository, ContextoUsuario contexto, Auditoria auditoria, PublicadorEventos eventos) {
        this.repository=repository; this.contexto=contexto; this.auditoria=auditoria; this.eventos=eventos;
    }
    public Organizacao consultar() {
        return repository.buscar(contexto.organizacaoId()).orElseThrow(()->new RecursoNaoEncontradoException("Organizacao nao cadastrada"));
    }
    @Transactional
    @PreAuthorize("hasRole('ADMIN_PLATAFORMA')")
    public UUID criar(UUID id, String nome, String codigo) {
        if (repository.buscar(id).isPresent()) throw new RegraNegocioException("ORGANIZACAO_EXISTENTE","Organizacao ja cadastrada");
        gravar(id,new Organizacao(nome,codigo));
        return id;
    }
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO')")
    public void atualizar(String nome, String codigo, boolean ativa) {
        consultar();
        var org = new Organizacao(nome,codigo);
        if (!ativa) org.inativar();
        gravar(contexto.organizacaoId(),org);
    }
    private void gravar(UUID id, Organizacao org) {
        repository.salvar(id,org);
        auditoria.registrar(id,contexto.usuarioId(),"ORGANIZACAO_SALVA",id.toString());
        eventos.publicar(new OrganizacaoAlterada(UUID.randomUUID(),Instant.now(),id,1,org.getCodigo(),org.getNome(),org.isAtiva()));
    }
    private UUID organizacaoAtiva() {
        if (!consultar().isAtiva()) throw new RegraNegocioException("ORGANIZACAO_INATIVA","Organizacao inativa");
        return contexto.organizacaoId();
    }
    public List<Setor> setores() { return repository.setores(organizacaoAtiva()); }
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO')")
    public void salvarSetor(String codigo, String nome) {
        var org=organizacaoAtiva();
        repository.salvarSetor(org,new Setor(codigo,nome));
        auditoria.registrar(org,contexto.usuarioId(),"SETOR_SALVO",codigo);
    }
    @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO')")
    public List<Usuario> usuarios() { return repository.usuarios(organizacaoAtiva()); }
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO')")
    public void salvarUsuario(String codigo, String nome, String email, PerfilUsuario perfil, boolean ativo) {
        if (perfil==PerfilUsuario.ADMIN_PLATAFORMA) throw new RegraNegocioException("PERFIL_INVALIDO","Perfil global gerenciado somente no Keycloak");
        var org=organizacaoAtiva();
        var usuario=new Usuario(codigo,nome,email,perfil,org.toString());
        if (!ativo) usuario.inativar();
        repository.salvarUsuario(org,usuario);
        auditoria.registrar(org,contexto.usuarioId(),"USUARIO_SALVO",codigo);
    }
}
