package com.almoxarifado.estoque.infrastructure.persistence;
import java.util.*;
import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.almoxarifado.estoque.application.*;
import com.almoxarifado.estoque.events.SaldoAlterado;
import com.almoxarifado.shared.application.SnapshotEventos;
@Repository
public class SnapshotEstoqueJdbc implements SnapshotRepository {
 private final JdbcTemplate jdbc;private final EstoqueRepository estoque;
 public SnapshotEstoqueJdbc(JdbcTemplate jdbc,EstoqueRepository estoque) { this.jdbc=jdbc;this.estoque=estoque; }
 public List<SnapshotEventos> carregar(UUID org,String recurso,UUID apos,int tamanho) {
  if(!recurso.equals("saldos")) throw new IllegalArgumentException("Recurso invalido");
  var lote=jdbc.queryForList("SELECT id,item_id FROM estoque.posicoes WHERE organizacao_id=? AND (?::uuid IS NULL OR id>?) ORDER BY id LIMIT ?",org,apos,apos,tamanho);
  lote.stream().map(r->(UUID)r.get("item_id")).distinct().sorted().forEach(item->estoque.bloquear(org,item));
  var resultado=new ArrayList<SnapshotEventos>();
  for(var row:lote) resultado.addAll(jdbc.query("SELECT * FROM estoque.posicoes WHERE organizacao_id=? AND id=?",(rs,n)->{
   var id=rs.getObject("id",UUID.class);
   return new SnapshotEventos(id,List.of(new SaldoAlterado(UUID.randomUUID(),Instant.now(),org,1,id,rs.getObject("item_id",UUID.class),rs.getObject("local_id",UUID.class),rs.getBigDecimal("quantidade"),rs.getBigDecimal("reservado"),rs.getString("lote"),rs.getObject("validade",LocalDate.class),rs.getString("serie"),rs.getBigDecimal("custo"),rs.getLong("revisao"))));
  },org,row.get("id")));
  return resultado;
 }
}
