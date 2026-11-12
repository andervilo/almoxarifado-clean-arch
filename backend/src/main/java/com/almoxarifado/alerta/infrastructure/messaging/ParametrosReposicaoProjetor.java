package com.almoxarifado.alerta.infrastructure.messaging;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import com.almoxarifado.catalogo.events.ParametrosReposicaoAlterados;
@Component
public class ParametrosReposicaoProjetor {
 private final JdbcTemplate jdbc;
 public ParametrosReposicaoProjetor(JdbcTemplate jdbc) {this.jdbc=jdbc;}
 @ApplicationModuleListener public void receber(ParametrosReposicaoAlterados e) {
  jdbc.update("""
   INSERT INTO alerta.parametros_reposicao(organizacao_id,item_id,almoxarifado_id,minimo,maximo,atualizado_em) VALUES(?,?,?,?,?,?)
   ON CONFLICT(organizacao_id,item_id,almoxarifado_id) DO UPDATE SET minimo=excluded.minimo,maximo=excluded.maximo,atualizado_em=excluded.atualizado_em
   WHERE alerta.parametros_reposicao.atualizado_em<=excluded.atualizado_em
   """,e.organizacaoId(),e.itemId(),e.almoxarifadoId(),e.minimo(),e.maximo(),java.sql.Timestamp.from(e.occurredAt()));
 }
}
