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
class InventarioCegoIntegrationTest extends SuporteOperacional {
 private void locais(Cenario c) {await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT count(*) FROM inventario.locais_projecao WHERE organizacao_id=?",Integer.class,c.org())).isEqualTo(2));}
 private void finalizar(Cenario c,String inv,String esperado) throws Exception {
  mvc.perform(post("/api/v1/inventarios/"+inv+"/finalizar").with(usuario(c.org())).contentType(MediaType.APPLICATION_JSON).content("{\"justificativa\":\"Contagem fisica auditada\"}")).andExpect(status().isAccepted());
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT status FROM inventario.inventarios WHERE organizacao_id=? AND id=?",String.class,c.org(),UUID.fromString(inv))).isEqualTo(esperado));
 }
 @Test void contaMaterialSemPosicaoEConcluiInventarioCego() throws Exception {
  var c=cenario("NENHUM");locais(c);
  var inv=criar(c.org(),"/inventarios",Map.of("localId",c.destino(),"contagemCega",true));
  var pos=json.readTree(mvc.perform(post("/api/v1/inventarios/"+inv+"/contagens").with(usuario(c.org())).contentType(MediaType.APPLICATION_JSON)
   .content(json.writeValueAsString(Map.of("itemId",c.item(),"quantidade",3,"custo",2)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("posicaoId").asText();
  mvc.perform(get("/api/v1/inventarios/"+inv).with(usuario(c.org()))).andExpect(jsonPath("$.contagens[0].sistema").isEmpty());
  finalizar(c,inv,"CONCLUIDO");
  assertThat(jdbc.queryForObject("SELECT quantidade FROM estoque.posicoes WHERE organizacao_id=? AND id=?",BigDecimal.class,c.org(),UUID.fromString(pos))).isEqualByComparingTo("3");
  mvc.perform(get("/api/v1/inventarios/"+inv).with(usuario(c.org()))).andExpect(jsonPath("$.contagens[0].diferenca").value(3));
 }
 @Test void ocultaSaldoEsperadoAteConclusao() throws Exception {
  var c=cenario("NENHUM");locais(c);assertThat(movimento(c,"entradas",UUID.randomUUID(),comando(c,10))).isEqualTo(201);
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT count(*) FROM inventario.saldos_projecao WHERE organizacao_id=?",Integer.class,c.org())).isEqualTo(1));
  var inv=criar(c.org(),"/inventarios",Map.of("localId",c.origem(),"contagemCega",true));
  var response=json.readTree(mvc.perform(get("/api/v1/inventarios/"+inv).with(usuario(c.org()))).andExpect(jsonPath("$.contagens[0].sistema").isEmpty()).andExpect(jsonPath("$.contagens[0].diferenca").isEmpty()).andReturn().getResponse().getContentAsString());
  var pos=response.get("contagens").get(0).get("posicaoId").asText();
  mvc.perform(put("/api/v1/inventarios/"+inv+"/contagens/"+pos).with(usuario(c.org())).contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\":8}")).andExpect(status().isOk());
  finalizar(c,inv,"CONCLUIDO");mvc.perform(get("/api/v1/inventarios/"+inv).with(usuario(c.org()))).andExpect(jsonPath("$.contagens[0].sistema").value(10)).andExpect(jsonPath("$.contagens[0].diferenca").value(-2));
 }
 @Test void novaPosicaoDuranteContagemCausaConflitoSemAjusteParcial() throws Exception {
  var c=cenario("NENHUM");locais(c);var inv=criar(c.org(),"/inventarios",Map.of("localId",c.destino()));
  var entrada=comando(c,5);entrada.put("destinoId",c.destino());assertThat(movimento(c,"entradas",UUID.randomUUID(),entrada)).isEqualTo(201);
  finalizar(c,inv,"CONFLITO");
  assertThat(jdbc.queryForObject("SELECT sum(quantidade) FROM estoque.posicoes WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("5");
 }
}
