package com.almoxarifado.catalogo.domain;
import java.util.*;
import java.math.BigDecimal;
public record Item(UUID id, UUID organizacaoId, String codigo, String descricao, UUID categoriaId,
 UUID unidadeMedidaId, TipoRastreabilidade rastreabilidade, BigDecimal estoqueMinimo, BigDecimal estoqueMaximo, boolean ativo) {
 public Item {
  Objects.requireNonNull(id); Objects.requireNonNull(organizacaoId); Objects.requireNonNull(categoriaId);
  Objects.requireNonNull(unidadeMedidaId); Objects.requireNonNull(rastreabilidade);
  if (codigo==null || codigo.isBlank() || descricao==null || descricao.isBlank()) throw new IllegalArgumentException("Codigo e descricao obrigatorios");
  if (estoqueMinimo==null || estoqueMaximo==null || estoqueMinimo.signum()<0 || estoqueMaximo.compareTo(estoqueMinimo)<0)
   throw new IllegalArgumentException("Limites de estoque invalidos");
 }
}
