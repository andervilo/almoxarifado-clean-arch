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
  if(recurso.equals("reservas")) return reservas(org,apos,tamanho);
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
 private List<SnapshotEventos> reservas(UUID org,UUID apos,int tamanho) {
  var lote=jdbc.queryForList("SELECT id,requisicao_id,item_id FROM estoque.revisoes_reserva WHERE organizacao_id=? AND (?::uuid IS NULL OR id>?) ORDER BY id LIMIT ?",org,apos,apos,tamanho);
  lote.stream().map(r->(UUID)r.get("item_id")).distinct().sorted().forEach(item->estoque.bloquear(org,item));
  var resultado=new ArrayList<SnapshotEventos>();
  for(var r:lote) {
   var req=(UUID)r.get("requisicao_id");var item=(UUID)r.get("item_id");
   long revisao=jdbc.queryForObject("SELECT revisao FROM estoque.revisoes_reserva WHERE organizacao_id=? AND id=?",Long.class,org,r.get("id"));
   var alocacoes=estoque.alocacoes(org,req,item).stream().map(a->new com.almoxarifado.requisicao.events.AlocacoesRequisicaoAtualizadas.Alocacao(a.posicaoId(),a.localId(),a.lote(),a.serie(),a.validade(),a.quantidade())).toList();
   resultado.add(new SnapshotEventos((UUID)r.get("id"),List.of(new com.almoxarifado.requisicao.events.AlocacoesRequisicaoAtualizadas(UUID.randomUUID(),Instant.now(),org,1,req,item,revisao,alocacoes))));
  }
  return resultado;
 }
}
