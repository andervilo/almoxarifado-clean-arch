package com.almoxarifado.estrutura.application;
import java.util.*;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import com.almoxarifado.estrutura.domain.*;
import com.almoxarifado.estrutura.events.*;
import com.almoxarifado.shared.application.*;
import com.almoxarifado.shared.domain.*;
@Service
@Transactional(readOnly=true)
@PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','REQUISITANTE','GESTOR')")
public class EstruturaService {
 private final EstruturaRepository repository;
 private final ContextoUsuario contexto;
 private final Auditoria auditoria;
 private final PublicadorEventos eventos;
 public EstruturaService(EstruturaRepository repository,ContextoUsuario contexto,Auditoria auditoria,PublicadorEventos eventos) {
  this.repository=repository; this.contexto=contexto; this.auditoria=auditoria; this.eventos=eventos;
 }
 public UUID organizacaoId() {
  var org=contexto.organizacaoId();
  if(!repository.organizacaoAtiva(org)) throw new RegraNegocioException("ORGANIZACAO_INDISPONIVEL","Organizacao inativa ou ainda nao sincronizada");
  return org;
 }
 private void pagina(int pagina,int tamanho) {
  if(pagina<0 || pagina>1000000 || tamanho<1 || tamanho>100) throw new IllegalArgumentException("Paginacao invalida");
 }
 public Almoxarifado buscarAlmoxarifado(UUID id) {
  contexto.exigirAlmoxarifado(id);
  return repository.almoxarifado(organizacaoId(),id).orElseThrow(()->new RecursoNaoEncontradoException("Almoxarifado nao encontrado"));
 }
 public LocalArmazenamento buscarLocal(UUID id) {
  var local=repository.local(organizacaoId(),id).orElseThrow(()->new RecursoNaoEncontradoException("Local nao encontrado"));
  contexto.exigirAlmoxarifado(local.almoxarifadoId());return local;
 }
 public List<Almoxarifado> almoxarifados(int pagina,int tamanho) { pagina(pagina,tamanho); return repository.almoxarifados(organizacaoId(),tamanho,pagina*tamanho,contexto.almoxarifadosPermitidos()); }
 public List<LocalArmazenamento> locais(UUID almox,int pagina,int tamanho) { pagina(pagina,tamanho); buscarAlmoxarifado(almox); return repository.locais(organizacaoId(),almox,tamanho,pagina*tamanho); }
 @Transactional
 @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE')")
 public Almoxarifado salvarAlmoxarifado(UUID id,boolean atualizar,String codigo,String nome,boolean ativo) {
  contexto.exigirAlmoxarifado(id);
  var org=organizacaoId(); if(atualizar) buscarAlmoxarifado(id);
  var valor=new Almoxarifado(id,org,codigo,nome,ativo);
  repository.salvar(valor);
  auditoria.registrar(org,contexto.usuarioId(),"ALMOXARIFADO_SALVO",id.toString());
  eventos.publicar(new AlmoxarifadoAlterado(UUID.randomUUID(),Instant.now(),org,1,id,ativo));
  return valor;
 }
 @Transactional
 @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE')")
 public LocalArmazenamento salvarLocal(UUID id,UUID almox,boolean atualizar,String codigo,String descricao,boolean ativo) {
  var org=organizacaoId();
  if(!buscarAlmoxarifado(almox).ativo()) throw new RegraNegocioException("ALMOXARIFADO_INATIVO","Almoxarifado inativo");
  if(atualizar && !buscarLocal(id).almoxarifadoId().equals(almox)) throw new RegraNegocioException("LOCAL_IMUTAVEL","Local nao pode mudar de almoxarifado");
  var valor=new LocalArmazenamento(id,org,almox,codigo,descricao,ativo);
  repository.salvar(valor);
  auditoria.registrar(org,contexto.usuarioId(),"LOCAL_SALVO",id.toString());
  eventos.publicar(new LocalAlterado(UUID.randomUUID(),Instant.now(),org,1,id,almox,ativo));
  return valor;
 }
}
