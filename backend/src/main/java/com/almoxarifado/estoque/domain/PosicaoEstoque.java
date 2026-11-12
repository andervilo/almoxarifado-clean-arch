package com.almoxarifado.estoque.domain;
import java.util.*;
import java.math.*;
import java.time.*;
import com.almoxarifado.shared.domain.RegraNegocioException;
public record PosicaoEstoque(UUID id,UUID organizacaoId,UUID itemId,UUID localId,String lote,String serie,
 LocalDate validade,Instant recebidoEm,BigDecimal quantidade,BigDecimal reservado,BigDecimal custo) {
 public PosicaoEstoque {
  Objects.requireNonNull(id); Objects.requireNonNull(organizacaoId); Objects.requireNonNull(itemId); Objects.requireNonNull(localId);
  Objects.requireNonNull(lote); Objects.requireNonNull(serie); Objects.requireNonNull(recebidoEm);
  if(quantidade==null || reservado==null || custo==null || quantidade.signum()<0 || reservado.signum()<0 || reservado.compareTo(quantidade)>0 || custo.signum()<0)
   throw new RegraNegocioException("SALDO_INVALIDO","Saldo, reserva ou custo invalido");
 }
 public BigDecimal disponivel() { return quantidade.subtract(reservado); }
 public PosicaoEstoque alterar(BigDecimal delta,BigDecimal reserva,BigDecimal novoCusto) {
  return new PosicaoEstoque(id,organizacaoId,itemId,localId,lote,serie,validade,recebidoEm,quantidade.add(delta),reservado.add(reserva),novoCusto);
 }
 public void validarSaida(LocalDate hoje) {
  if(validade!=null && validade.isBefore(hoje)) throw new RegraNegocioException("LOTE_VENCIDO","Lote vencido");
 }
}
