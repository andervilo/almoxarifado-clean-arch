package com.almoxarifado.inventario.domain;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record Contagem(UUID posicaoId,UUID itemId,UUID localId,String lote,String serie,LocalDate validade,
 BigDecimal sistema,BigDecimal contada,BigDecimal custo,long revisao) {
 public Contagem contar(BigDecimal valor) {
  if(valor==null || valor.signum()<0 || valor.scale()>4) throw new IllegalArgumentException("Contagem invalida");
  return new Contagem(posicaoId,itemId,localId,lote,serie,validade,sistema,valor,custo,revisao);
 }
 public BigDecimal diferenca() { return contada==null?null:contada.subtract(sistema); }
}
