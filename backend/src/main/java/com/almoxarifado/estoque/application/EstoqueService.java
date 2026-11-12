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
  return motor.executar(contexto.organizacaoId(),contexto.usuarioId(),id,comando);
 }
 public List<PosicaoEstoque> saldos(UUID item) { return repository.posicoes(contexto.organizacaoId(),item); }
 public List<Map<String,Object>> historico(UUID item,int pagina,int tamanho) {
  if(pagina<0 || pagina>1000000 || tamanho<1 || tamanho>100) throw new IllegalArgumentException("Paginacao invalida");
  return repository.historico(contexto.organizacaoId(),item,tamanho,pagina*tamanho);
 }
 public List<Map<String,Object>> rastrear(UUID item,String lote,String serie,int pagina,int tamanho) {
  if(pagina<0 || pagina>1000000 || tamanho<1 || tamanho>100) throw new IllegalArgumentException("Paginacao invalida");
  return repository.rastrear(contexto.organizacaoId(),item,lote,serie,tamanho,pagina*tamanho);
 }
 public List<Map<String,Object>> custos(UUID item) { return repository.custos(contexto.organizacaoId(),item); }
}
