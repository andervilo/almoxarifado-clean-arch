package com.almoxarifado.requisicao.domain;
import java.util.*;
import java.math.BigDecimal;
import com.almoxarifado.shared.domain.RegraNegocioException;
public record Requisicao(UUID id,UUID organizacaoId,String solicitanteId,UUID localId,List<Linha> linhas,
 String status,UUID comandoId,String erro) {
 public record Linha(UUID itemId,BigDecimal quantidade) {
  public Linha {
   Objects.requireNonNull(itemId);
   if(quantidade==null || quantidade.signum()<=0 || quantidade.scale()>4) throw new IllegalArgumentException("Quantidade solicitada invalida");
  }
 }
 public Requisicao {
  Objects.requireNonNull(id);Objects.requireNonNull(organizacaoId);Objects.requireNonNull(localId);
  if(solicitanteId==null || solicitanteId.isBlank() || linhas==null || linhas.isEmpty() || linhas.size()>100) throw new IllegalArgumentException("Solicitante e linhas obrigatorios");
  linhas=List.copyOf(linhas);
  if(linhas.stream().map(Linha::itemId).distinct().count()!=linhas.size()) throw new IllegalArgumentException("Item duplicado na requisicao");
 }
 public Requisicao transicionar(String destino,UUID comando,String falha,String... permitidos) {
  if(!Set.of(permitidos).contains(status)) throw new RegraNegocioException("TRANSICAO_INVALIDA","Estado atual: "+status);
  return new Requisicao(id,organizacaoId,solicitanteId,localId,linhas,destino,comando,falha);
 }
}
