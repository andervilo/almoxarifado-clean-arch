package com.almoxarifado.catalogo.infrastructure.messaging;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import com.almoxarifado.estrutura.events.AlmoxarifadoAlterado;
@Component("catalogoAlmoxarifadoProjetor")
public class AlmoxarifadoProjetor {
 private final JdbcTemplate jdbc;
 public AlmoxarifadoProjetor(JdbcTemplate jdbc) {this.jdbc=jdbc;}
 @ApplicationModuleListener public void receber(AlmoxarifadoAlterado e) {
  jdbc.update("""
   INSERT INTO catalogo.almoxarifados_projecao(organizacao_id,id,ativo,atualizado_em) VALUES(?,?,?,?)
   ON CONFLICT(organizacao_id,id) DO UPDATE SET ativo=excluded.ativo,atualizado_em=excluded.atualizado_em
   WHERE catalogo.almoxarifados_projecao.atualizado_em<=excluded.atualizado_em
   """,e.organizacaoId(),e.almoxarifadoId(),e.ativo(),java.sql.Timestamp.from(e.occurredAt()));
 }
}
