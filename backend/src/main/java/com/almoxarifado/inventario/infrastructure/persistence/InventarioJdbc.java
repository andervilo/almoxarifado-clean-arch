package com.almoxarifado.inventario.infrastructure.persistence;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.*;
import com.almoxarifado.inventario.application.InventarioRepository;
import com.almoxarifado.inventario.domain.Contagem;
@Repository
public class InventarioJdbc implements InventarioRepository {
 private final JdbcTemplate jdbc;
 public InventarioJdbc(JdbcTemplate jdbc) { this.jdbc=jdbc; }
 private static final RowMapper<Contagem> CONTAGEM=(rs,n)->new Contagem(rs.getObject("posicao_id",UUID.class),rs.getObject("item_id",UUID.class),rs.getObject("local_id",UUID.class),rs.getString("lote"),rs.getString("serie"),rs.getObject("validade",java.time.LocalDate.class),rs.getBigDecimal("sistema"),rs.getBigDecimal("contada"),rs.getBigDecimal("custo"),rs.getLong("revisao"));
 public List<Contagem> snapshot(UUID org,UUID local) {
  return jdbc.query("SELECT id AS posicao_id,item_id,local_id,lote,serie,validade,quantidade AS sistema,NULL::numeric AS contada,custo,revisao FROM inventario.saldos_projecao WHERE organizacao_id=? AND local_id=? ORDER BY id LIMIT 1001",CONTAGEM,org,local);
 }
 public void criar(UUID org,UUID id,UUID local,List<Contagem> contagens,boolean contagemCega) {
  jdbc.update("INSERT INTO inventario.inventarios(organizacao_id,id,local_id,status,contagem_cega) VALUES(?,?,?,'CONTAGEM',?)",org,id,local,contagemCega);
  for(var c:contagens) jdbc.update("""
   INSERT INTO inventario.contagens(organizacao_id,inventario_id,posicao_id,item_id,local_id,lote,serie,validade,sistema,custo,revisao)
   VALUES(?,?,?,?,?,?,?,?,?,?,?)
   """,org,id,c.posicaoId(),c.itemId(),c.localId(),c.lote(),c.serie(),c.validade(),c.sistema(),c.custo(),c.revisao());
 }
 public Optional<Inventario> buscar(UUID org,UUID id,boolean lock) {
  return jdbc.query("SELECT * FROM inventario.inventarios WHERE organizacao_id=? AND id=?"+(lock?" FOR UPDATE":""),(rs,n)->new Inventario(id,rs.getObject("local_id",UUID.class),rs.getString("status"),rs.getObject("comando_id",UUID.class),rs.getString("erro"),jdbc.query("SELECT * FROM inventario.contagens WHERE organizacao_id=? AND inventario_id=? ORDER BY posicao_id",CONTAGEM,org,id),rs.getBoolean("contagem_cega")),org,id).stream().findFirst();
 }
 public void contar(UUID org,UUID inv,Contagem c) { jdbc.update("UPDATE inventario.contagens SET contada=? WHERE organizacao_id=? AND inventario_id=? AND posicao_id=?",c.contada(),org,inv,c.posicaoId()); }
 public void estado(UUID org,UUID id,String status,UUID comando,String erro) { jdbc.update("UPDATE inventario.inventarios SET status=?,comando_id=?,erro=? WHERE organizacao_id=? AND id=?",status,comando,erro,org,id); }
 public List<UUID> listar(UUID org,int limite,int deslocamento,Set<UUID> escopo) {
  String ids=com.almoxarifado.shared.infrastructure.persistence.FiltroEscopoSql.array(escopo);
  return jdbc.query("SELECT i.id FROM inventario.inventarios i LEFT JOIN inventario.locais_projecao l ON l.organizacao_id=i.organizacao_id AND l.id=i.local_id WHERE i.organizacao_id=? AND (?::uuid[] IS NULL OR l.almoxarifado_id=ANY(?::uuid[])) ORDER BY i.criado_em DESC,i.id LIMIT ? OFFSET ?",(rs,n)->rs.getObject(1,UUID.class),org,ids,ids,limite,deslocamento);
 }
 public UUID almoxarifadoDoLocal(UUID org,UUID local) {
  return jdbc.query("SELECT almoxarifado_id FROM inventario.locais_projecao WHERE organizacao_id=? AND id=?",(rs,n)->rs.getObject(1,UUID.class),org,local).stream().findFirst().orElse(null);
 }
 public void adicionarContagem(UUID org,UUID inv,Contagem c) {
  jdbc.update("INSERT INTO inventario.contagens(organizacao_id,inventario_id,posicao_id,item_id,local_id,lote,serie,validade,sistema,contada,custo,revisao) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",org,inv,c.posicaoId(),c.itemId(),c.localId(),c.lote(),c.serie(),c.validade(),c.sistema(),c.contada(),c.custo(),c.revisao());
 }
}
