package com.almoxarifado.estrutura.infrastructure.messaging;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.modulith.events.ApplicationModuleListener;
import com.almoxarifado.organizacao.events.OrganizacaoAlterada;
@Component("estruturaOrganizacaoProjetor")
public class OrganizacaoProjetor {
 private final JdbcTemplate jdbc;
 public OrganizacaoProjetor(JdbcTemplate jdbc) { this.jdbc=jdbc; }
 @ApplicationModuleListener
 public void receber(OrganizacaoAlterada evento) {
  jdbc.update("""
   INSERT INTO estrutura.organizacoes_projecao (id,ativa,atualizado_em) VALUES (?,?,?)
   ON CONFLICT (id) DO UPDATE SET ativa=excluded.ativa,atualizado_em=excluded.atualizado_em
   WHERE estrutura.organizacoes_projecao.atualizado_em <= excluded.atualizado_em
   """,evento.organizacaoId(),evento.ativa(),java.sql.Timestamp.from(evento.occurredAt()));
 }
}
