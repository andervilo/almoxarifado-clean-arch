package com.almoxarifado.catalogo.domain;
import java.util.*;
import java.math.BigDecimal;
public record ParametrosReposicao(UUID id,UUID itemId,UUID almoxarifadoId,BigDecimal minimo,BigDecimal maximo) {
 public ParametrosReposicao {
  Objects.requireNonNull(id);Objects.requireNonNull(itemId);Objects.requireNonNull(almoxarifadoId);
  if(minimo==null || maximo==null || minimo.signum()<0 || maximo.compareTo(minimo)<0 || minimo.scale()>4 || maximo.scale()>4)
   throw new IllegalArgumentException("Limites de reposicao invalidos");
 }
}
