package com.almoxarifado.organizacao.domain;
import java.util.*;
import com.almoxarifado.shared.domain.RegraNegocioException;
public record DadosConta(UUID keycloakId,String username,String nome,String email,PerfilUsuario perfil,boolean ativo,Set<UUID> almoxarifados) {
 public DadosConta {
  if(username==null || !username.matches("[a-zA-Z0-9._-]{3,120}") || nome==null || nome.isBlank() || nome.length()>255 || email==null || email.isBlank() || email.length()>255 || perfil==null) throw new IllegalArgumentException("Dados de conta invalidos");
  if(perfil==PerfilUsuario.ADMIN_PLATAFORMA) throw new RegraNegocioException("CONTA_PROTEGIDA","Administradores globais sao geridos no Keycloak");
  username=username.toLowerCase(java.util.Locale.ROOT);
  if(almoxarifados!=null) {if(almoxarifados.size()>1000 || almoxarifados.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Escopo invalido");almoxarifados=Set.copyOf(almoxarifados);}
 }
 public String assinatura() {
  String scope=almoxarifados==null?"TODOS":almoxarifados.stream().map(UUID::toString).sorted().collect(java.util.stream.Collectors.joining(","));
  return java.util.stream.Stream.of(keycloakId==null?"":keycloakId.toString(),username,nome,email,perfil.name(),Boolean.toString(ativo),scope).map(s->s.length()+":"+s).collect(java.util.stream.Collectors.joining());
 }
}
