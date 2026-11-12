package com.almoxarifado.inventario.application;
import java.util.*;
import java.time.Instant;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import com.almoxarifado.shared.events.AjusteInventarioSolicitado;
import com.almoxarifado.shared.application.*;
import com.almoxarifado.shared.domain.*;
@Service @Transactional(readOnly=true)
@PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','GESTOR')")
public class InventarioService {
 private final InventarioRepository repository;private final ContextoUsuario contexto;private final Auditoria auditoria;private final PublicadorEventos eventos;
 public InventarioService(InventarioRepository repository,ContextoUsuario contexto,Auditoria auditoria,PublicadorEventos eventos) { this.repository=repository;this.contexto=contexto;this.auditoria=auditoria;this.eventos=eventos; }
 private InventarioRepository.Inventario buscar(UUID id,boolean lock) { return repository.buscar(contexto.organizacaoId(),id,lock).orElseThrow(()->new RecursoNaoEncontradoException("Inventario nao encontrado")); }
 public InventarioRepository.Inventario buscar(UUID id) { return buscar(id,false); }
 public List<InventarioRepository.Inventario> listar(int pagina,int tamanho) {
  if(pagina<0 || pagina>1000000 || tamanho<1 || tamanho>100) throw new IllegalArgumentException("Paginacao invalida");
  return repository.listar(contexto.organizacaoId(),tamanho,pagina*tamanho).stream().map(this::buscar).toList();
 }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE')")
 public InventarioRepository.Inventario criar(UUID local) {
  var org=contexto.organizacaoId();var snapshot=repository.snapshot(org,local);
  if(snapshot.isEmpty()) throw new RegraNegocioException("SALDO_NAO_SINCRONIZADO","Local sem posicoes de estoque sincronizadas");
  var id=UUID.randomUUID();repository.criar(org,id,local,snapshot);auditoria.registrar(org,contexto.usuarioId(),"INVENTARIO_CRIADO",id.toString());return buscar(id);
 }
 private void exigirContagem(InventarioRepository.Inventario inv) { if(!inv.status().equals("CONTAGEM")) throw new RegraNegocioException("ESTADO_INVALIDO","Inventario nao esta em contagem"); }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE')")
 public void contar(UUID id,UUID posicao,BigDecimal quantidade) {
  var inv=buscar(id,true);exigirContagem(inv);
  var c=inv.contagens().stream().filter(v->v.posicaoId().equals(posicao)).findFirst().orElseThrow(()->new RecursoNaoEncontradoException("Posicao fora do inventario"));
  repository.contar(contexto.organizacaoId(),id,c.contar(quantidade));auditoria.registrar(contexto.organizacaoId(),contexto.usuarioId(),"INVENTARIO_CONTADO",id+":"+posicao);
 }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE')")
 public void finalizar(UUID id,String justificativa) {
  if(justificativa==null || justificativa.isBlank()) throw new IllegalArgumentException("Justificativa obrigatoria");
  var inv=buscar(id,true);exigirContagem(inv);
  if(inv.contagens().stream().anyMatch(c->c.contada()==null)) throw new RegraNegocioException("CONTAGEM_INCOMPLETA","Todas as posicoes precisam ser contadas");
  var comando=UUID.randomUUID();var org=contexto.organizacaoId();repository.estado(org,id,"FINALIZANDO",comando,null);
  eventos.publicar(new AjusteInventarioSolicitado(comando,Instant.now(),org,1,id,inv.localId(),contexto.usuarioId(),justificativa,inv.contagens().stream()
   .map(c->new AjusteInventarioSolicitado.Linha(c.posicaoId(),c.itemId(),c.revisao(),c.sistema(),c.contada(),c.lote(),c.serie(),c.validade(),c.custo())).toList()));
  auditoria.registrar(org,contexto.usuarioId(),"INVENTARIO_FINALIZANDO",id.toString());
 }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE')")
 public void cancelar(UUID id) {
  var inv=buscar(id,true);
  if(!Set.of("CONTAGEM","CONFLITO").contains(inv.status())) throw new RegraNegocioException("ESTADO_INVALIDO","Inventario nao pode ser cancelado");
  repository.estado(contexto.organizacaoId(),id,"CANCELADO",null,null);auditoria.registrar(contexto.organizacaoId(),contexto.usuarioId(),"INVENTARIO_CANCELADO",id.toString());
 }
}
