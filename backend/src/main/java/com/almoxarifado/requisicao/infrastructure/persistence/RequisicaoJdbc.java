package com.almoxarifado.requisicao.infrastructure.persistence;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import com.almoxarifado.requisicao.domain.Requisicao;
import com.almoxarifado.requisicao.application.RequisicaoRepository;
@Repository
public class RequisicaoJdbc implements RequisicaoRepository {
 private final JdbcTemplate jdbc;
 public RequisicaoJdbc(JdbcTemplate jdbc) { this.jdbc=jdbc; }
 private RowMapper<Requisicao> mapper() {
  return (rs,n)->{
   var org=rs.getObject("organizacao_id",UUID.class);var id=rs.getObject("id",UUID.class);
   var linhas=jdbc.query("SELECT * FROM requisicao.linhas WHERE organizacao_id=? AND requisicao_id=? ORDER BY item_id",
    (r,i)->new Requisicao.Linha(r.getObject("item_id",UUID.class),r.getBigDecimal("quantidade")),org,id);
   return new Requisicao(id,org,rs.getString("solicitante_id"),rs.getObject("local_id",UUID.class),linhas,rs.getString("status"),rs.getObject("comando_id",UUID.class),rs.getString("erro"));
  };
 }
 public void criar(Requisicao r) {
  jdbc.update("INSERT INTO requisicao.requisicoes(organizacao_id,id,solicitante_id,local_id,status) VALUES(?,?,?,?,?)",r.organizacaoId(),r.id(),r.solicitanteId(),r.localId(),r.status());
  for(var l:r.linhas()) jdbc.update("INSERT INTO requisicao.linhas(organizacao_id,requisicao_id,item_id,quantidade) VALUES(?,?,?,?)",r.organizacaoId(),r.id(),l.itemId(),l.quantidade());
 }
 public Optional<Requisicao> buscar(UUID org,UUID id,boolean bloquear) {
  return jdbc.query("SELECT * FROM requisicao.requisicoes WHERE organizacao_id=? AND id=?"+(bloquear?" FOR UPDATE":""),mapper(),org,id).stream().findFirst();
 }
 public List<Requisicao> listar(UUID org,String solicitante,int limite,int deslocamento,Set<UUID> escopo) {
  String ids=com.almoxarifado.shared.infrastructure.persistence.FiltroEscopoSql.array(escopo);
  return jdbc.query("""
   SELECT r.* FROM requisicao.requisicoes r LEFT JOIN requisicao.locais_projecao l ON l.organizacao_id=r.organizacao_id AND l.id=r.local_id
   WHERE r.organizacao_id=? AND (?::text IS NULL OR r.solicitante_id=?) AND (?::uuid[] IS NULL OR l.almoxarifado_id=ANY(?::uuid[]))
   ORDER BY r.criado_em DESC,r.id LIMIT ? OFFSET ?
   """,mapper(),org,solicitante,solicitante,ids,ids,limite,deslocamento);
 }
 public void atualizar(Requisicao r) {
  jdbc.update("UPDATE requisicao.requisicoes SET status=?,comando_id=?,erro=? WHERE organizacao_id=? AND id=?",r.status(),r.comandoId(),r.erro(),r.organizacaoId(),r.id());
 }
 public void editar(Requisicao r) {
  jdbc.update("UPDATE requisicao.requisicoes SET local_id=?,erro=NULL WHERE organizacao_id=? AND id=?",r.localId(),r.organizacaoId(),r.id());
  jdbc.update("DELETE FROM requisicao.linhas WHERE organizacao_id=? AND requisicao_id=?",r.organizacaoId(),r.id());
  for(var l:r.linhas()) jdbc.update("INSERT INTO requisicao.linhas(organizacao_id,requisicao_id,item_id,quantidade) VALUES(?,?,?,?)",r.organizacaoId(),r.id(),l.itemId(),l.quantidade());
 }
 public UUID almoxarifadoDoLocal(UUID org,UUID local) {
  return jdbc.query("SELECT almoxarifado_id FROM requisicao.locais_projecao WHERE organizacao_id=? AND id=?",(rs,n)->rs.getObject(1,UUID.class),org,local).stream().findFirst().orElse(null);
 }
}
