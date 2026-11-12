package com.almoxarifado.alerta.infrastructure.messaging;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import com.almoxarifado.catalogo.events.ItemAlterado;
import com.almoxarifado.estoque.events.SaldoAlterado;
@Component
public class AlertaProjetor {
 private final JdbcTemplate jdbc;
 public AlertaProjetor(JdbcTemplate jdbc) { this.jdbc=jdbc; }
 @ApplicationModuleListener public void configuracao(com.almoxarifado.organizacao.events.ConfiguracaoOrganizacaoAlterada e) {
  jdbc.update("""
   INSERT INTO alerta.configuracoes(organizacao_id,dias,atualizado_em) VALUES(?,?,?)
   ON CONFLICT(organizacao_id) DO UPDATE SET dias=excluded.dias,atualizado_em=excluded.atualizado_em
   WHERE alerta.configuracoes.atualizado_em<=excluded.atualizado_em
   """,e.organizacaoId(),e.diasAlertaValidade(),java.sql.Timestamp.from(e.occurredAt()));
 }
 @ApplicationModuleListener public void item(ItemAlterado e) {
  jdbc.update("""
   INSERT INTO alerta.itens_projecao(organizacao_id,id,ativo,minimo,atualizado_em) VALUES(?,?,?,?,?)
   ON CONFLICT(organizacao_id,id) DO UPDATE SET ativo=excluded.ativo,minimo=excluded.minimo,atualizado_em=excluded.atualizado_em
   WHERE alerta.itens_projecao.atualizado_em<=excluded.atualizado_em
   """,e.organizacaoId(),e.itemId(),e.ativo(),e.estoqueMinimo(),java.sql.Timestamp.from(e.occurredAt()));
 }
 @ApplicationModuleListener public void saldo(SaldoAlterado e) {
  jdbc.update("""
   INSERT INTO alerta.saldos_projecao(organizacao_id,id,item_id,local_id,quantidade,reservado,validade,revisao) VALUES(?,?,?,?,?,?,?,?)
   ON CONFLICT(organizacao_id,id) DO UPDATE SET quantidade=excluded.quantidade,reservado=excluded.reservado,revisao=excluded.revisao
   WHERE alerta.saldos_projecao.revisao<excluded.revisao
   """,e.organizacaoId(),e.posicaoId(),e.itemId(),e.localId(),e.quantidade(),e.reservado(),e.validade(),e.revisao());
 }
}
