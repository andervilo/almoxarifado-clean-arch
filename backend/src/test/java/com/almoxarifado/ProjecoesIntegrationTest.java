package com.almoxarifado;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.almoxarifado.estoque.events.SaldoAlterado;
class ProjecoesIntegrationTest extends SuporteOperacional {
 @Autowired ApplicationEventPublisher eventos;
 @Autowired PlatformTransactionManager transacoes;
 @Test void duplicatasEEventosAntigosNaoRegridemSaldosProjetados() {
  UUID org=UUID.randomUUID(),pos=UUID.randomUUID(),item=UUID.randomUUID(),local=UUID.randomUUID();
  var novo=new SaldoAlterado(UUID.randomUUID(),Instant.now(),org,1,pos,item,local,BigDecimal.TEN,BigDecimal.ZERO,"",null,"",BigDecimal.ONE,5);
  var antigo=new SaldoAlterado(UUID.randomUUID(),Instant.now(),org,1,pos,item,local,new BigDecimal("99"),BigDecimal.ZERO,"",null,"",BigDecimal.ONE,1);
  new TransactionTemplate(transacoes).executeWithoutResult(s->{ eventos.publishEvent(novo);eventos.publishEvent(novo);eventos.publishEvent(antigo); });
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->{
   for(String modulo:List.of("inventario","alerta")) {
    assertThat(jdbc.queryForObject("SELECT quantidade FROM "+modulo+".saldos_projecao WHERE organizacao_id=? AND id=?",BigDecimal.class,org,pos)).isEqualByComparingTo("10");
    assertThat(jdbc.queryForObject("SELECT revisao FROM "+modulo+".saldos_projecao WHERE organizacao_id=? AND id=?",Long.class,org,pos)).isEqualTo(5L);
   }
  });
 }
}
