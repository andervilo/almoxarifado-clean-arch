package com.almoxarifado.estoque.application;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import com.almoxarifado.shared.application.ContextoUsuario;
import com.almoxarifado.estoque.domain.PosicaoEstoque;
@Service
@Transactional(readOnly=true)
@PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','GESTOR','REQUISITANTE')")
public class EstoqueService {
 private final EstoqueRepository repository;
 private final MotorEstoque motor;
 private final ContextoUsuario contexto;
 public EstoqueService(EstoqueRepository repository,MotorEstoque motor,ContextoUsuario contexto) {
  this.repository=repository; this.motor=motor; this.contexto=contexto;
 }
 @Transactional
 @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE')")
 public UUID movimentar(UUID id,ComandoEstoque comando) {
  if(!Set.of("ENTRADA","SAIDA","TRANSFERENCIA","AJUSTE","ESTORNO").contains(comando.tipo())) throw new IllegalArgumentException("Tipo invalido para API");
  if(contexto.almoxarifadosPermitidos()!=null) {
   switch(comando.tipo()) {
    case "ENTRADA","AJUSTE" -> exigirLocal(comando.destinoId());
    case "SAIDA" -> exigirLocal(comando.origemId());
    case "TRANSFERENCIA" -> { exigirLocal(comando.origemId());exigirLocal(comando.destinoId()); }
    case "ESTORNO" -> {
     var posicoes=repository.posicoes(contexto.organizacaoId(),comando.itemId());
     for(var linha:repository.linhas(contexto.organizacaoId(),comando.referenciaId())) {
      var p=posicoes.stream().filter(v->v.id().equals(linha.posicaoId())).findFirst().orElseThrow(()->new com.almoxarifado.shared.domain.RecursoNaoEncontradoException("Movimento nao encontrado"));
      exigirLocal(p.localId());
     }
    }
   }
  }
  return motor.executar(contexto.organizacaoId(),contexto.usuarioId(),id,comando);
 }
 private void exigirLocal(UUID local) { contexto.exigirAlmoxarifado(repository.almoxarifadoDoLocal(contexto.organizacaoId(),local)); }
 public List<PosicaoEstoque> saldos(UUID item) {
  var org=contexto.organizacaoId();
  var escopo=contexto.almoxarifadosPermitidos();var posicoes=repository.posicoes(org,item);
  if(escopo==null) return posicoes;
  return posicoes.stream().filter(p->{var almox=repository.almoxarifadoDoLocal(org,p.localId());return almox!=null && escopo.contains(almox);}).toList();
 }
 public List<Map<String,Object>> historico(UUID item,int pagina,int tamanho) {
  if(pagina<0 || pagina>1000000 || tamanho<1 || tamanho>100) throw new IllegalArgumentException("Paginacao invalida");
  return repository.historico(contexto.organizacaoId(),item,tamanho,pagina*tamanho,contexto.almoxarifadosPermitidos());
 }
 public List<Map<String,Object>> rastrear(UUID item,String lote,String serie,int pagina,int tamanho) {
  if(pagina<0 || pagina>1000000 || tamanho<1 || tamanho>100) throw new IllegalArgumentException("Paginacao invalida");
  return repository.rastrear(contexto.organizacaoId(),item,lote,serie,tamanho,pagina*tamanho,contexto.almoxarifadosPermitidos());
 }
 public List<Map<String,Object>> custos(UUID item) { return repository.custos(contexto.organizacaoId(),item,contexto.almoxarifadosPermitidos()); }
}
