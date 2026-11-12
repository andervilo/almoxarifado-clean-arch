package com.almoxarifado.organizacao.application;
import java.util.*;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import com.almoxarifado.organizacao.domain.*;
import com.almoxarifado.organizacao.events.ProvisionamentoIdentidadeSolicitado;
import com.almoxarifado.shared.application.*;
import com.almoxarifado.shared.domain.*;
@Service @Transactional(readOnly=true)
@PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO')")
public class ProvisionarIdentidade {
 private final ProvisionamentoRepository repository;private final OrganizacaoRepository organizacoes;private final ProvedorIdentidade provedor;
 private final ContextoUsuario usuario;private final PublicadorEventos eventos;private final Auditoria auditoria;
 public ProvisionarIdentidade(ProvisionamentoRepository repository,OrganizacaoRepository organizacoes,ProvedorIdentidade provedor,ContextoUsuario usuario,PublicadorEventos eventos,Auditoria auditoria) {
  this.repository=repository;this.organizacoes=organizacoes;this.provedor=provedor;this.usuario=usuario;this.eventos=eventos;this.auditoria=auditoria;
 }
 public ProvisionamentoIdentidade buscar(UUID id) {return repository.buscar(usuario.organizacaoId(),id,false).orElseThrow(()->new RecursoNaoEncontradoException("Provisionamento nao encontrado"));}
 @Transactional public ProvisionamentoIdentidade solicitar(UUID id,DadosConta dados) {
  if(!provedor.habilitado()) throw new IntegracaoIndisponivelException("Provisionamento administrativo do Keycloak nao configurado");
  var org=usuario.organizacaoId();if(!organizacoes.buscar(org).orElseThrow(()->new RecursoNaoEncontradoException("Organizacao nao cadastrada")).isAtiva()) throw new RegraNegocioException("ORGANIZACAO_INATIVA","Organizacao inativa");
  repository.bloquear(org,id);var existente=repository.buscar(org,id,true);
  if(existente.isPresent()) {if(!existente.get().dados().assinatura().equals(dados.assinatura())) throw new RegraNegocioException("IDEMPOTENCIA_CONFLITO","Chave usada para outro pedido");return existente.get();}
  var pedido=new ProvisionamentoIdentidade(id,org,usuario.usuarioId(),dados,"PENDENTE",null,null);repository.criar(pedido);publicar(org,id);
  auditoria.registrar(org,usuario.usuarioId(),"IDENTIDADE_SOLICITADA",id.toString());return pedido;
 }
 @Transactional public ProvisionamentoIdentidade reprocessar(UUID id) {
  if(!provedor.habilitado()) throw new IntegracaoIndisponivelException("Keycloak administrativo nao configurado");
  var org=usuario.organizacaoId();var pedido=repository.buscar(org,id,true).orElseThrow(()->new RecursoNaoEncontradoException("Provisionamento nao encontrado"));
  if(java.util.Set.of("CONCLUIDO","SUPERADO").contains(pedido.status())) return pedido;
  if(pedido.status().equals("FALHA")) repository.estado(org,id,"PENDENTE",pedido.keycloakId(),null);
  publicar(org,id);auditoria.registrar(org,usuario.usuarioId(),"IDENTIDADE_REPROCESSADA",id.toString());return buscar(id);
 }
 private void publicar(UUID org,UUID id) {eventos.publicar(new ProvisionamentoIdentidadeSolicitado(UUID.randomUUID(),Instant.now(),org,1,id));}
}
