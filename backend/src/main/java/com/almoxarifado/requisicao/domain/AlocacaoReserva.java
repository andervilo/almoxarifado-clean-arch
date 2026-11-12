package com.almoxarifado.requisicao.domain;
import java.util.UUID;
import java.math.BigDecimal;
public record AlocacaoReserva(UUID posicaoId,BigDecimal quantidade) {
 public AlocacaoReserva {
  if(posicaoId==null || quantidade==null || quantidade.signum()<=0 || quantidade.scale()>4) throw new IllegalArgumentException("Alocacao invalida");
 }
}
