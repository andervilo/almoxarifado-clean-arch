package com.almoxarifado;
import static org.assertj.core.api.Assertions.*;
import java.util.UUID;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import com.almoxarifado.estoque.application.ComandoEstoque;
@Testcontainers
class MigracaoConsumoIntegrationTest {
 @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine");
 @Test void recuperaVinculoHistoricoSemAlterarMovimentoOriginal() {
  var fonte=new DriverManagerDataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword());
  Flyway.configure().dataSource(fonte).schemas("estoque").defaultSchema("estoque").locations("classpath:db/migration/estoque").target("4").load().migrate();
  var jdbc=new JdbcTemplate(fonte);UUID org=UUID.randomUUID(),item=UUID.randomUUID(),pos=UUID.randomUUID(),op=UUID.randomUUID(),req=UUID.randomUUID();
  jdbc.update("INSERT INTO estoque.posicoes(organizacao_id,id,item_id,local_id,lote,serie,recebido_em,quantidade,reservado,custo) VALUES(?,?,?,?,'','',CURRENT_TIMESTAMP,8,0,3)",org,pos,item,UUID.randomUUID());
  var comando=new ComandoEstoque("ENTREGA",item,UUID.randomUUID(),null,new BigDecimal("2"),null,null,null,null,"Pedido historico",req);
  jdbc.update("INSERT INTO estoque.operacoes(organizacao_id,id,item_id,tipo,assinatura,usuario_id,motivo) VALUES(?,?,?,'ENTREGA',?,'usuario','Pedido historico')",org,op,item,comando.toString());
  jdbc.update("INSERT INTO estoque.linhas_operacao(organizacao_id,operacao_id,posicao_id,quantidade,custo) VALUES(?,?,?,-2,3)",org,op,pos);
  Flyway.configure().dataSource(fonte).schemas("estoque").defaultSchema("estoque").locations("classpath:db/migration/estoque").load().migrate();
  assertThat(jdbc.queryForObject("SELECT requisicao_id FROM estoque.entregas_requisicao WHERE organizacao_id=?",UUID.class,org)).isEqualTo(req);
  assertThat(jdbc.queryForObject("SELECT quantidade FROM estoque.entregas_requisicao WHERE organizacao_id=?",BigDecimal.class,org)).isEqualByComparingTo("2");
  assertThat(jdbc.queryForObject("SELECT custo_total FROM estoque.entregas_requisicao WHERE organizacao_id=?",BigDecimal.class,org)).isEqualByComparingTo("6");
  assertThat(jdbc.queryForObject("SELECT assinatura FROM estoque.operacoes WHERE organizacao_id=?",String.class,org)).isEqualTo(comando.toString());
 }
}
