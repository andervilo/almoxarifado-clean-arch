package com.almoxarifado.estrutura.domain;
import java.util.*;
public record LocalArmazenamento(UUID id, UUID organizacaoId, UUID almoxarifadoId, String codigo, String descricao, boolean ativo) {
 public LocalArmazenamento {
  Objects.requireNonNull(id); Objects.requireNonNull(organizacaoId); Objects.requireNonNull(almoxarifadoId);
  if(codigo==null || codigo.isBlank() || descricao==null || descricao.isBlank()) throw new IllegalArgumentException("Codigo e descricao obrigatorios");
 }
}
