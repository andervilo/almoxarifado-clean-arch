package com.almoxarifado.requisicao.infrastructure.messaging;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import com.almoxarifado.organizacao.events.SetorAlterado;
import com.almoxarifado.requisicao.events.EntregaRequisicaoRegistrada;
@Component
public class ConsumoSetorProjetor {
 private final JdbcTemplate jdbc;
 public ConsumoSetorProjetor(JdbcTemplate jdbc) {this.jdbc=jdbc;}
 @ApplicationModuleListener public void setor(SetorAlterado e) {
  jdbc.update("""
   INSERT INTO requisicao.setores_projecao(organizacao_id,codigo,nome,centro_custo,atualizado_em) VALUES(?,?,?,?,?)
   ON CONFLICT(organizacao_id,codigo) DO UPDATE SET nome=excluded.nome,centro_custo=excluded.centro_custo,atualizado_em=excluded.atualizado_em
   WHERE requisicao.setores_projecao.atualizado_em<=excluded.atualizado_em
   """,e.organizacaoId(),e.codigo(),e.nome(),e.centroCusto(),java.sql.Timestamp.from(e.occurredAt()));
 }
 @ApplicationModuleListener public void entrega(EntregaRequisicaoRegistrada e) {
  jdbc.update("INSERT INTO requisicao.consumo(organizacao_id,operacao_id,requisicao_id,item_id,quantidade,custo_total,efetivado_em) VALUES(?,?,?,?,?,?,?) ON CONFLICT DO NOTHING",e.organizacaoId(),e.operacaoId(),e.requisicaoId(),e.itemId(),e.quantidade(),e.custoTotal(),java.sql.Timestamp.from(e.efetivadoEm()));
 }
}
