package com.almoxarifado.catalogo.infrastructure.persistence;
import java.util.*;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.almoxarifado.catalogo.application.SnapshotRepository;
import com.almoxarifado.catalogo.events.ItemAlterado;
import com.almoxarifado.shared.application.SnapshotEventos;
@Repository
public class SnapshotCatalogoJdbc implements SnapshotRepository {
 private final JdbcTemplate jdbc;
 public SnapshotCatalogoJdbc(JdbcTemplate jdbc) { this.jdbc=jdbc; }
 public List<SnapshotEventos> carregar(UUID org,String recurso,UUID apos,int tamanho) {
  if(recurso.equals("parametros-reposicao")) return jdbc.query("SELECT * FROM catalogo.parametros_reposicao WHERE organizacao_id=? AND (?::uuid IS NULL OR id>?) ORDER BY id LIMIT ? FOR UPDATE",(rs,n)->new SnapshotEventos(rs.getObject("id",UUID.class),List.of(
   new com.almoxarifado.catalogo.events.ParametrosReposicaoAlterados(UUID.randomUUID(),Instant.now(),org,1,rs.getObject("item_id",UUID.class),rs.getObject("almoxarifado_id",UUID.class),rs.getBigDecimal("minimo"),rs.getBigDecimal("maximo")))),org,apos,apos,tamanho);
  if(!recurso.equals("itens")) throw new IllegalArgumentException("Recurso invalido");
  return jdbc.query("SELECT * FROM catalogo.itens WHERE organizacao_id=? AND (?::uuid IS NULL OR id>?) ORDER BY id LIMIT ? FOR UPDATE",(rs,n)->{
   var id=rs.getObject("id",UUID.class);
   return new SnapshotEventos(id,List.of(new ItemAlterado(UUID.randomUUID(),Instant.now(),org,1,id,rs.getString("codigo"),rs.getString("descricao"),rs.getString("rastreabilidade"),rs.getBigDecimal("estoque_minimo"),rs.getBigDecimal("estoque_maximo"),rs.getBoolean("ativo"))));
  },org,apos,apos,tamanho);
 }
}
