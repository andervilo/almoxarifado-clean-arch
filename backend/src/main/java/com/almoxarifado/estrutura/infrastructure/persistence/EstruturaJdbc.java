package com.almoxarifado.estrutura.infrastructure.persistence;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.*;
import com.almoxarifado.estrutura.application.EstruturaRepository;
import com.almoxarifado.estrutura.domain.*;
@Repository
public class EstruturaJdbc implements EstruturaRepository {
 private final JdbcTemplate jdbc;
 public EstruturaJdbc(JdbcTemplate jdbc) { this.jdbc=jdbc; }
 private static final RowMapper<Almoxarifado> ALMOX=(rs,n)->new Almoxarifado(rs.getObject("id",UUID.class),rs.getObject("organizacao_id",UUID.class),rs.getString("codigo"),rs.getString("nome"),rs.getBoolean("ativo"));
 private static final RowMapper<LocalArmazenamento> LOCAL=(rs,n)->new LocalArmazenamento(rs.getObject("id",UUID.class),rs.getObject("organizacao_id",UUID.class),rs.getObject("almoxarifado_id",UUID.class),rs.getString("codigo"),rs.getString("descricao"),rs.getBoolean("ativo"));
 public boolean organizacaoAtiva(UUID org) {
  return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM estrutura.organizacoes_projecao WHERE id=? AND ativa)",Boolean.class,org));
 }
 public Optional<Almoxarifado> almoxarifado(UUID org,UUID id) {
  return jdbc.query("SELECT * FROM estrutura.almoxarifados WHERE organizacao_id=? AND id=?",ALMOX,org,id).stream().findFirst();
 }
 public Optional<LocalArmazenamento> local(UUID org,UUID id) {
  return jdbc.query("SELECT * FROM estrutura.locais WHERE organizacao_id=? AND id=?",LOCAL,org,id).stream().findFirst();
 }
 public List<Almoxarifado> almoxarifados(UUID org,int limite,int deslocamento) {
  return jdbc.query("SELECT * FROM estrutura.almoxarifados WHERE organizacao_id=? ORDER BY codigo,id LIMIT ? OFFSET ?",ALMOX,org,limite,deslocamento);
 }
 public List<LocalArmazenamento> locais(UUID org,UUID almox,int limite,int deslocamento) {
  return jdbc.query("SELECT * FROM estrutura.locais WHERE organizacao_id=? AND almoxarifado_id=? ORDER BY codigo,id LIMIT ? OFFSET ?",LOCAL,org,almox,limite,deslocamento);
 }
 public void salvar(Almoxarifado v) {
  jdbc.update("""
   INSERT INTO estrutura.almoxarifados(organizacao_id,id,codigo,nome,ativo) VALUES (?,?,?,?,?)
   ON CONFLICT(organizacao_id,id) DO UPDATE SET codigo=excluded.codigo,nome=excluded.nome,ativo=excluded.ativo
   """,v.organizacaoId(),v.id(),v.codigo(),v.nome(),v.ativo());
 }
 public void salvar(LocalArmazenamento v) {
  jdbc.update("""
   INSERT INTO estrutura.locais(organizacao_id,id,almoxarifado_id,codigo,descricao,ativo) VALUES (?,?,?,?,?,?)
   ON CONFLICT(organizacao_id,id) DO UPDATE SET codigo=excluded.codigo,descricao=excluded.descricao,ativo=excluded.ativo
   """,v.organizacaoId(),v.id(),v.almoxarifadoId(),v.codigo(),v.descricao(),v.ativo());
 }
}
