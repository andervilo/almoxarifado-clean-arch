package com.almoxarifado;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import java.time.Duration;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
class InventarioFluxoIntegrationTest extends SuporteOperacional {
 private String criarInventario(Cenario c) throws Exception {
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT count(*) FROM inventario.saldos_projecao WHERE organizacao_id=?",Integer.class,c.org())).isEqualTo(1));
  return criar(c.org(),"/inventarios",Map.of("localId",c.origem()));
 }
 private void contar(Cenario c,String inv,int quantidade) throws Exception {
  var pos=jdbc.queryForObject("SELECT id FROM estoque.posicoes WHERE organizacao_id=?",UUID.class,c.org());
  mvc.perform(put("/api/v1/inventarios/"+inv+"/contagens/"+pos).with(usuario(c.org())).contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":"+quantidade+"}")).andExpect(status().isOk());
 }
 private void finalizar(Cenario c,String inv,String esperado) throws Exception {
  mvc.perform(post("/api/v1/inventarios/"+inv+"/finalizar").with(usuario(c.org())).contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"Contagem fisica validada\"}")).andExpect(status().isAccepted());
  await().atMost(Duration.ofSeconds(15)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT status FROM inventario.inventarios WHERE organizacao_id=? AND id=?",String.class,c.org(),UUID.fromString(inv))).isEqualTo(esperado));
 }
 @Test void contagemAjustaSaldoComJustificativaEAuditoria() throws Exception {
  var c=cenario("NENHUM");assertThat(movimento(c,"entradas",UUID.randomUUID(),comando(c,10))).isEqualTo(201);
  var inv=criarInventario(c);
  mvc.perform(post("/api/v1/inventarios/"+inv+"/finalizar").with(usuario(c.org())).contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"Contagem\"}")).andExpect(status().isUnprocessableEntity());
  contar(c,inv,8);finalizar(c,inv,"CONCLUIDO");
  assertThat(jdbc.queryForObject("SELECT quantidade FROM estoque.posicoes WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("8");
  assertThat(jdbc.queryForObject("SELECT count(*) FROM estoque.operacoes WHERE organizacao_id=? AND tipo='AJUSTE'",Integer.class,c.org())).isEqualTo(1);
  mvc.perform(get("/api/v1/inventarios/"+inv).with(usuario(UUID.randomUUID()))).andExpect(status().isNotFound());
 }
 @Test void movimentoDuranteContagemImpedeSobrescritaDeSaldo() throws Exception {
  var c=cenario("NENHUM");assertThat(movimento(c,"entradas",UUID.randomUUID(),comando(c,10))).isEqualTo(201);
  var inv=criarInventario(c);contar(c,inv,8);
  assertThat(movimento(c,"entradas",UUID.randomUUID(),comando(c,1))).isEqualTo(201);
  finalizar(c,inv,"CONFLITO");
  assertThat(jdbc.queryForObject("SELECT quantidade FROM estoque.posicoes WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("11");
  assertThat(jdbc.queryForObject("SELECT count(*) FROM estoque.operacoes WHERE organizacao_id=? AND tipo='AJUSTE'",Integer.class,c.org())).isZero();
 }
}
