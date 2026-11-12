package com.almoxarifado.organizacao.infrastructure.persistence;
import java.util.*;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.almoxarifado.organizacao.application.SnapshotRepository;
import com.almoxarifado.organizacao.events.*;
import com.almoxarifado.shared.application.SnapshotEventos;
@Repository
public class SnapshotOrganizacaoJdbc implements SnapshotRepository {
 private final JdbcTemplate jdbc;private final OrganizacaoJdbc organizacoes;
 public SnapshotOrganizacaoJdbc(JdbcTemplate jdbc,OrganizacaoJdbc organizacoes) { this.jdbc=jdbc;this.organizacoes=organizacoes; }
 public List<SnapshotEventos> carregar(UUID org,String recurso,UUID apos,int tamanho) {
  if(recurso.equals("setores")) return jdbc.query("SELECT * FROM organizacao.setores WHERE organizacao_id=? AND (?::uuid IS NULL OR snapshot_id>?) ORDER BY snapshot_id LIMIT ? FOR UPDATE",(rs,n)->new SnapshotEventos(rs.getObject("snapshot_id",UUID.class),List.of(new SetorAlterado(UUID.randomUUID(),Instant.now(),org,1,rs.getString("codigo"),rs.getString("nome"),rs.getString("centro_custo")))),org,apos,apos,tamanho);
  if(!recurso.equals("organizacao")) throw new IllegalArgumentException("Recurso invalido");
  if(apos!=null) return List.of();
  return jdbc.query("SELECT * FROM organizacao.organizacoes WHERE id=? FOR UPDATE",(rs,n)->new SnapshotEventos(org,List.of(
   new OrganizacaoAlterada(UUID.randomUUID(),Instant.now(),org,1,rs.getString("codigo"),rs.getString("nome"),rs.getBoolean("ativa")),
   new ConfiguracaoOrganizacaoAlterada(UUID.randomUUID(),Instant.now(),org,1,organizacoes.configuracao(org).diasAlertaValidade()))),org);
 }
}
