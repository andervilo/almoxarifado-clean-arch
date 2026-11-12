package com.almoxarifado.requisicao.domain;
import java.util.*;
import java.math.BigDecimal;
import com.almoxarifado.shared.domain.RegraNegocioException;
public record Requisicao(UUID id,UUID organizacaoId,String solicitanteId,UUID localId,List<Linha> linhas,
 String status,UUID comandoId,String erro,String estadoAnterior) {
 public Requisicao(UUID id,UUID org,String solicitante,UUID local,List<Linha> linhas,String status,UUID comando,String erro) {
  this(id,org,solicitante,local,linhas,status,comando,erro,null);
 }
 public record Linha(UUID itemId,BigDecimal quantidade,BigDecimal atendida) {
  public Linha(UUID itemId,BigDecimal quantidade) {this(itemId,quantidade,BigDecimal.ZERO);}
  public Linha {
   Objects.requireNonNull(itemId);
   if(quantidade==null || quantidade.signum()<=0 || quantidade.scale()>4) throw new IllegalArgumentException("Quantidade solicitada invalida");
   if(atendida==null || atendida.signum()<0 || atendida.compareTo(quantidade)>0) throw new IllegalArgumentException("Quantidade atendida invalida");
  }
  public BigDecimal pendente() {return quantidade.subtract(atendida);}
  public Linha entregar(BigDecimal valor) {
   if(valor==null || valor.signum()<=0 || valor.compareTo(pendente())>0 || valor.scale()>4)
    throw new RegraNegocioException("ENTREGA_INVALIDA","Quantidade excede o restante do pedido");
   return new Linha(itemId,quantidade,atendida.add(valor));
  }
 }
 public Requisicao {
  Objects.requireNonNull(id);Objects.requireNonNull(organizacaoId);Objects.requireNonNull(localId);
  if(solicitanteId==null || solicitanteId.isBlank() || linhas==null || linhas.isEmpty() || linhas.size()>100) throw new IllegalArgumentException("Solicitante e linhas obrigatorios");
  linhas=List.copyOf(linhas);
  if(linhas.stream().map(Linha::itemId).distinct().count()!=linhas.size()) throw new IllegalArgumentException("Item duplicado na requisicao");
 }
 public Requisicao transicionar(String destino,UUID comando,String falha,String... permitidos) {
  exigirEstado(permitidos);
  return new Requisicao(id,organizacaoId,solicitanteId,localId,linhas,destino,comando,falha,comando==null?null:status);
 }
 public void exigirEstado(String... permitidos) {
  if(!Set.of(permitidos).contains(status)) throw new RegraNegocioException("TRANSICAO_INVALIDA","Estado atual: "+status);
 }
}
