package com.almoxarifado.estrutura.infrastructure.persistence;
import java.util.*;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.almoxarifado.estrutura.application.SnapshotRepository;
import com.almoxarifado.estrutura.events.*;
import com.almoxarifado.shared.application.SnapshotEventos;
@Repository
public class SnapshotEstruturaJdbc implements SnapshotRepository {
 private final JdbcTemplate jdbc;
 public SnapshotEstruturaJdbc(JdbcTemplate jdbc) { this.jdbc=jdbc; }
 public List<SnapshotEventos> carregar(UUID org,String recurso,UUID apos,int tamanho) {
  String tabela=switch(recurso) {case "almoxarifados"->"almoxarifados";case "locais"->"locais";default->throw new IllegalArgumentException("Recurso invalido");};
  return jdbc.query("SELECT * FROM estrutura."+tabela+" WHERE organizacao_id=? AND (?::uuid IS NULL OR id>?) ORDER BY id LIMIT ? FOR UPDATE",(rs,n)->{
   var id=rs.getObject("id",UUID.class);
   return new SnapshotEventos(id,List.of(recurso.equals("almoxarifados")
    ?new AlmoxarifadoAlterado(UUID.randomUUID(),Instant.now(),org,1,id,rs.getBoolean("ativo"))
    :new LocalAlterado(UUID.randomUUID(),Instant.now(),org,1,id,rs.getObject("almoxarifado_id",UUID.class),rs.getBoolean("ativo"))));
  },org,apos,apos,tamanho);
 }
}
