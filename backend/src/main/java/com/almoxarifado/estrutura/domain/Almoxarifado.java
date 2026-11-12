package com.almoxarifado.estrutura.domain;
import java.util.*;
public record Almoxarifado(UUID id, UUID organizacaoId, String codigo, String nome, boolean ativo) {
 public Almoxarifado {
  Objects.requireNonNull(id); Objects.requireNonNull(organizacaoId);
  if(codigo==null || codigo.isBlank() || nome==null || nome.isBlank()) throw new IllegalArgumentException("Codigo e nome obrigatorios");
 }
}
