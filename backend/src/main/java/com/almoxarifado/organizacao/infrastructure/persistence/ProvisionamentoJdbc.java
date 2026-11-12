package com.almoxarifado.organizacao.infrastructure.persistence;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.almoxarifado.organizacao.application.ProvisionamentoRepository;
import com.almoxarifado.organizacao.domain.*;
@Repository
public class ProvisionamentoJdbc implements ProvisionamentoRepository {
 private final JdbcTemplate jdbc;private final ObjectMapper json;
 public ProvisionamentoJdbc(JdbcTemplate jdbc,ObjectMapper json) {this.jdbc=jdbc;this.json=json;}
 public void bloquear(UUID org,UUID id) {jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,"identidade:"+org+":"+id);}
 public Optional<ProvisionamentoIdentidade> buscar(UUID org,UUID id,boolean lock) {
  return jdbc.query("SELECT * FROM organizacao.provisionamentos_identidade WHERE organizacao_id=? AND id=?"+(lock?" FOR UPDATE":""),(rs,n)->{
   try {return new ProvisionamentoIdentidade(id,org,rs.getString("ator"),json.readValue(rs.getString("dados"),DadosConta.class),rs.getString("status"),rs.getObject("keycloak_id",UUID.class),rs.getString("erro"));}
   catch(com.fasterxml.jackson.core.JsonProcessingException ex) {throw new IllegalStateException("Dados de provisionamento invalidos",ex);}
  },org,id).stream().findFirst();
 }
 public void criar(ProvisionamentoIdentidade p) {
  try {jdbc.update("INSERT INTO organizacao.provisionamentos_identidade(organizacao_id,id,ator,dados,status) VALUES(?,?,?,?,'PENDENTE')",p.organizacaoId(),p.id(),p.ator(),json.writeValueAsString(p.dados()));}
  catch(com.fasterxml.jackson.core.JsonProcessingException ex) {throw new IllegalStateException("Nao foi possivel serializar o pedido",ex);}
 }
 public void estado(UUID org,UUID id,String status,UUID keycloak,String erro) {jdbc.update("UPDATE organizacao.provisionamentos_identidade SET status=?,keycloak_id=?,erro=? WHERE organizacao_id=? AND id=?",status,keycloak,erro,org,id);}
 public void bloquearUsuario(UUID org,UUID id) {jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,"usuario-keycloak:"+org+":"+id);}
 public boolean possuiPosterior(UUID org,UUID id,UUID pedido) {
  return Boolean.TRUE.equals(jdbc.queryForObject("""
   SELECT EXISTS(SELECT 1 FROM organizacao.identidades_gerenciadas i
   JOIN organizacao.provisionamentos_identidade atual ON atual.organizacao_id=i.organizacao_id AND atual.id=i.ultimo_provisionamento
   JOIN organizacao.provisionamentos_identidade candidato ON candidato.organizacao_id=i.organizacao_id AND candidato.id=?
   WHERE i.organizacao_id=? AND i.keycloak_id=? AND (atual.criado_em,atual.id)>(candidato.criado_em,candidato.id))
   """,Boolean.class,pedido,org,id));
 }
 public boolean atual(UUID org,UUID id,UUID pedido) {return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM organizacao.identidades_gerenciadas WHERE organizacao_id=? AND keycloak_id=? AND ultimo_provisionamento=?)",Boolean.class,org,id,pedido));}
 public void marcarAtual(UUID org,UUID id,UUID pedido) {jdbc.update("INSERT INTO organizacao.identidades_gerenciadas(organizacao_id,keycloak_id,ultimo_provisionamento) VALUES(?,?,?) ON CONFLICT(organizacao_id,keycloak_id) DO UPDATE SET ultimo_provisionamento=excluded.ultimo_provisionamento",org,id,pedido);}
}
