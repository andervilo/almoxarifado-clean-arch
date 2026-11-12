package com.almoxarifado.estoque.infrastructure.messaging;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import com.almoxarifado.organizacao.events.OrganizacaoAlterada;
import com.almoxarifado.catalogo.events.ItemAlterado;
import com.almoxarifado.estrutura.events.*;
@Component("estoqueCadastrosProjetor")
public class CadastrosProjetor {
 private final JdbcTemplate jdbc;
 public CadastrosProjetor(JdbcTemplate jdbc) { this.jdbc=jdbc; }
 @ApplicationModuleListener public void organizacao(OrganizacaoAlterada e) {
  jdbc.update("""
   INSERT INTO estoque.organizacoes_projecao(id,ativa,atualizado_em) VALUES(?,?,?)
   ON CONFLICT(id) DO UPDATE SET ativa=excluded.ativa,atualizado_em=excluded.atualizado_em
   WHERE estoque.organizacoes_projecao.atualizado_em<=excluded.atualizado_em
   """,e.organizacaoId(),e.ativa(),java.sql.Timestamp.from(e.occurredAt()));
 }
 @ApplicationModuleListener public void item(ItemAlterado e) {
  jdbc.update("""
   INSERT INTO estoque.itens_projecao(organizacao_id,id,ativo,rastreabilidade,estoque_minimo,atualizado_em) VALUES(?,?,?,?,?,?)
   ON CONFLICT(organizacao_id,id) DO UPDATE SET ativo=excluded.ativo,rastreabilidade=excluded.rastreabilidade,estoque_minimo=excluded.estoque_minimo,atualizado_em=excluded.atualizado_em
   WHERE estoque.itens_projecao.atualizado_em<=excluded.atualizado_em
   """,e.organizacaoId(),e.itemId(),e.ativo(),e.rastreabilidade(),e.estoqueMinimo(),java.sql.Timestamp.from(e.occurredAt()));
 }
 @ApplicationModuleListener public void almoxarifado(AlmoxarifadoAlterado e) {
  jdbc.update("""
   INSERT INTO estoque.almoxarifados_projecao(organizacao_id,id,ativo,atualizado_em) VALUES(?,?,?,?)
   ON CONFLICT(organizacao_id,id) DO UPDATE SET ativo=excluded.ativo,atualizado_em=excluded.atualizado_em
   WHERE estoque.almoxarifados_projecao.atualizado_em<=excluded.atualizado_em
   """,e.organizacaoId(),e.almoxarifadoId(),e.ativo(),java.sql.Timestamp.from(e.occurredAt()));
 }
 @ApplicationModuleListener public void local(LocalAlterado e) {
  jdbc.update("""
   INSERT INTO estoque.locais_projecao(organizacao_id,id,almoxarifado_id,ativo,atualizado_em) VALUES(?,?,?,?,?)
   ON CONFLICT(organizacao_id,id) DO UPDATE SET ativo=excluded.ativo,atualizado_em=excluded.atualizado_em
   WHERE estoque.locais_projecao.atualizado_em<=excluded.atualizado_em
   """,e.organizacaoId(),e.localId(),e.almoxarifadoId(),e.ativo(),java.sql.Timestamp.from(e.occurredAt()));
 }
}
