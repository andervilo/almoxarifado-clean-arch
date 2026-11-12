package com.almoxarifado.catalogo.domain;
import java.util.*;
public record UnidadeMedida(UUID id, UUID organizacaoId, String codigo, String nome, boolean ativo) {
 public UnidadeMedida {
  Objects.requireNonNull(id); Objects.requireNonNull(organizacaoId);
  if (codigo==null || codigo.isBlank() || nome==null || nome.isBlank()) throw new IllegalArgumentException("Codigo e nome obrigatorios");
 }
}
