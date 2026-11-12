package com.almoxarifado.inventario.infrastructure.messaging;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import com.almoxarifado.estrutura.events.LocalAlterado;
@Component("inventarioLocalEscopoProjetor")
public class LocalEscopoProjetor {
 private final JdbcTemplate jdbc;
 public LocalEscopoProjetor(JdbcTemplate jdbc) { this.jdbc=jdbc; }
 @ApplicationModuleListener public void receber(LocalAlterado e) {
  jdbc.update("""
   INSERT INTO inventario.locais_projecao(organizacao_id,id,almoxarifado_id,atualizado_em) VALUES(?,?,?,?)
   ON CONFLICT(organizacao_id,id) DO UPDATE SET almoxarifado_id=excluded.almoxarifado_id,atualizado_em=excluded.atualizado_em
   WHERE inventario.locais_projecao.atualizado_em<=excluded.atualizado_em
   """,e.organizacaoId(),e.localId(),e.almoxarifadoId(),java.sql.Timestamp.from(e.occurredAt()));
 }
}
