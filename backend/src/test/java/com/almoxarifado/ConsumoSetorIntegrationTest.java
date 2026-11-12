package com.almoxarifado;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
class ConsumoSetorIntegrationTest extends SuporteOperacional {
 private void acao(Cenario c,String req,String acao) throws Exception {mvc.perform(post("/api/v1/requisicoes/"+req+"/"+acao).with(usuario(c.org()))).andExpect(status().isAccepted());}
 private void estado(Cenario c,String req,String estado) {await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT status FROM requisicao.requisicoes WHERE organizacao_id=? AND id=?",String.class,c.org(),UUID.fromString(req))).isEqualTo(estado));}
 @Test void consumoUsaEntregasEfetivasCentroHistoricoEEstornosSemDuplicarReplay() throws Exception {
  var c=cenario("NENHUM");
  mvc.perform(put("/api/v1/setores").with(usuario(c.org())).contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"S\",\"nome\":\"Compras\",\"centroCusto\":\"=1+1\"}")).andExpect(status().isOk());
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT count(*) FROM requisicao.setores_projecao WHERE organizacao_id=?",Integer.class,c.org())).isEqualTo(1));
  assertThat(movimento(c,"entradas",UUID.randomUUID(),comando(c,10))).isEqualTo(201);
  var req=criar(c.org(),"/requisicoes",Map.of("localId",c.origem(),"setorCodigo","S","linhas",List.of(Map.of("itemId",c.item(),"quantidade",2))));
  acao(c,req,"enviar");estado(c,req,"RESERVADA");acao(c,req,"separar");acao(c,req,"entregar");estado(c,req,"ENTREGUE");
  var hoje=LocalDate.now(ZoneOffset.UTC);var rota="/api/v1/relatorios/consumo-por-setor?inicio="+hoje+"&fim="+hoje;
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->mvc.perform(get(rota).with(usuario(c.org()))).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].quantidade").value(2)).andExpect(jsonPath("$[0].custo_total").value(4)));
  mvc.perform(put("/api/v1/setores").with(usuario(c.org())).contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"S\",\"nome\":\"Compras\",\"centroCusto\":\"NOVO\"}")).andExpect(status().isOk());
  mvc.perform(get(rota).with(usuario(c.org()))).andExpect(jsonPath("$[0].centro_custo").value("=1+1"));
  String csv=mvc.perform(get(rota.replace("?","/exportar?")).with(usuario(c.org()))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
  assertThat(csv).contains("\"'=1+1\"");
  mvc.perform(get(rota).with(usuario(UUID.randomUUID()))).andExpect(jsonPath("$.length()").value(0));
  var entrega=jdbc.queryForObject("SELECT id FROM estoque.operacoes WHERE organizacao_id=? AND tipo='ENTREGA'",UUID.class,c.org());
  var estorno=comando(c,2);estorno.put("referenciaId",entrega);assertThat(movimento(c,"estornos",UUID.randomUUID(),estorno)).isEqualTo(201);
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->mvc.perform(get(rota).with(usuario(c.org()))).andExpect(jsonPath("$[0].quantidade").value(0)).andExpect(jsonPath("$[0].custo_total").value(0)));
  jdbc.update("DELETE FROM requisicao.consumo WHERE organizacao_id=?",c.org());
  mvc.perform(post("/api/v1/estoque/sincronizacoes/entregas").with(usuario(c.org()))).andExpect(jsonPath("$.registros").value(2));
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT sum(quantidade) FROM requisicao.consumo WHERE organizacao_id=?",BigDecimal.class,c.org())).isZero());
 }
}
