package com.almoxarifado.requisicao.application;
import java.util.*;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import com.almoxarifado.requisicao.domain.Requisicao;
import com.almoxarifado.requisicao.events.OperacaoRequisicaoSolicitada;
import com.almoxarifado.shared.application.*;
import com.almoxarifado.shared.domain.RecursoNaoEncontradoException;
@Service @Transactional(readOnly=true)
@PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','REQUISITANTE','GESTOR')")
public class RequisicaoService {
 private final RequisicaoRepository repository;private final ContextoUsuario contexto;private final Auditoria auditoria;private final PublicadorEventos eventos;
 public RequisicaoService(RequisicaoRepository repository,ContextoUsuario contexto,Auditoria auditoria,PublicadorEventos eventos) {
  this.repository=repository;this.contexto=contexto;this.auditoria=auditoria;this.eventos=eventos;
 }
 private boolean restrito() { return contexto.possuiPerfil("REQUISITANTE") && !contexto.possuiPerfil("ADMIN_PLATAFORMA") && !contexto.possuiPerfil("ADMIN_ORGANIZACAO") && !contexto.possuiPerfil("ALMOXARIFE") && !contexto.possuiPerfil("GESTOR"); }
 private Requisicao buscar(UUID id,boolean lock) {
  var r=repository.buscar(contexto.organizacaoId(),id,lock).orElseThrow(()->new RecursoNaoEncontradoException("Requisicao nao encontrada"));
  if(restrito() && !r.solicitanteId().equals(contexto.usuarioId())) throw new RecursoNaoEncontradoException("Requisicao nao encontrada");
  return r;
 }
 public Requisicao buscar(UUID id) { return buscar(id,false); }
 public List<Requisicao> listar(int pagina,int tamanho) {
  if(pagina<0 || pagina>1000000 || tamanho<1 || tamanho>100) throw new IllegalArgumentException("Paginacao invalida");
  return repository.listar(contexto.organizacaoId(),restrito()?contexto.usuarioId():null,tamanho,pagina*tamanho);
 }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','REQUISITANTE')")
 public Requisicao criar(UUID local,List<Requisicao.Linha> linhas) {
  var r=new Requisicao(UUID.randomUUID(),contexto.organizacaoId(),contexto.usuarioId(),local,linhas,"RASCUNHO",null,null);
  repository.criar(r);auditoria.registrar(r.organizacaoId(),contexto.usuarioId(),"REQUISICAO_CRIADA",r.id().toString());return r;
 }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','REQUISITANTE')")
 public Requisicao editar(UUID id,UUID local,List<Requisicao.Linha> linhas) {
  var r=buscar(id,true);
  r.transicionar(r.status(),null,null,"RASCUNHO","FALHA_RESERVA");
  var novo=new Requisicao(id,r.organizacaoId(),r.solicitanteId(),local,linhas,r.status(),null,null);
  repository.editar(novo);auditoria.registrar(r.organizacaoId(),contexto.usuarioId(),"REQUISICAO_EDITADA",id.toString());return novo;
 }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','REQUISITANTE')")
 public Requisicao enviar(UUID id) { return solicitar(buscar(id,true),"RESERVA","RESERVANDO","RASCUNHO","FALHA_RESERVA"); }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE')")
 public Requisicao separar(UUID id) { return salvar(buscar(id,true).transicionar("SEPARADA",null,null,"RESERVADA")); }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE')")
 public Requisicao entregar(UUID id) { return solicitar(buscar(id,true),"ENTREGA","ENTREGANDO","SEPARADA"); }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','REQUISITANTE')")
 public Requisicao confirmar(UUID id) {
  var r=buscar(id,true);
  if(!r.solicitanteId().equals(contexto.usuarioId()) && !contexto.possuiPerfil("ADMIN_PLATAFORMA") && !contexto.possuiPerfil("ADMIN_ORGANIZACAO")) throw new RecursoNaoEncontradoException("Requisicao nao encontrada");
  return salvar(r.transicionar("FINALIZADA",null,null,"ENTREGUE"));
 }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','REQUISITANTE')")
 public Requisicao cancelar(UUID id) {
  var r=buscar(id,true);
  if(Set.of("RASCUNHO","FALHA_RESERVA").contains(r.status())) return salvar(r.transicionar("CANCELADA",null,null,"RASCUNHO","FALHA_RESERVA"));
  return solicitar(r,"LIBERACAO","CANCELANDO","RESERVADA","SEPARADA");
 }
 private Requisicao solicitar(Requisicao r,String tipo,String estado,String... permitidos) {
  var comando=UUID.randomUUID();var novo=r.transicionar(estado,comando,null,permitidos);
  salvar(novo);eventos.publicar(new OperacaoRequisicaoSolicitada(comando,Instant.now(),r.organizacaoId(),1,r.id(),contexto.usuarioId(),tipo,r.localId(),r.linhas().stream().map(l->new OperacaoRequisicaoSolicitada.Linha(l.itemId(),l.quantidade())).toList()));
  return novo;
 }
 private Requisicao salvar(Requisicao r) { repository.atualizar(r);auditoria.registrar(r.organizacaoId(),contexto.usuarioId(),"REQUISICAO_"+r.status(),r.id().toString());return r; }
}
