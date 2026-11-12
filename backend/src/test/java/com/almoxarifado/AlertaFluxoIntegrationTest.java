package com.almoxarifado;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
class AlertaFluxoIntegrationTest extends SuporteOperacional {
 @Test void prazoDeValidadeEhConfiguravelPorOrganizacao() throws Exception {
  var c=cenario("LOTE_VALIDADE");var body=comando(c,2);body.put("lote","PROXIMO");body.put("validade",LocalDate.now(ZoneOffset.UTC).plusDays(10).toString());
  assertThat(movimento(c,"entradas",UUID.randomUUID(),body)).isEqualTo(201);
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->mvc.perform(get("/api/v1/alertas").with(usuario(c.org()))).andExpect(jsonPath("$[0].tipo").value("VALIDADE_PROXIMA")));
  mvc.perform(put("/api/v1/organizacao/configuracoes").with(usuario(c.org())).contentType(MediaType.APPLICATION_JSON).content("{\"diasAlertaValidade\":5}")).andExpect(status().isOk());
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->mvc.perform(get("/api/v1/alertas").with(usuario(c.org()))).andExpect(jsonPath("$.length()").value(0)));
  mvc.perform(put("/api/v1/organizacao/configuracoes").with(usuario(c.org())).contentType(MediaType.APPLICATION_JSON).content("{\"diasAlertaValidade\":0}")).andExpect(status().isBadRequest());
 }
 @Test void alertaVencimentoMinimoEDispensaRespeitamTenant() throws Exception {
  var c=cenario("LOTE_VALIDADE");
  var body=comando(c,2);body.put("lote","VENCIDO");body.put("validade",LocalDate.now(ZoneOffset.UTC).minusDays(1).toString());
  assertThat(movimento(c,"entradas",UUID.randomUUID(),body)).isEqualTo(201);
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->{
   var resposta=mvc.perform(get("/api/v1/alertas").with(usuario(c.org()))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
   var alertas=json.readTree(resposta); assertThat(alertas.size()).isEqualTo(2);
   var tipos=new HashSet<String>();alertas.forEach(a->tipos.add(a.get("tipo").asText()));
   assertThat(tipos).containsExactlyInAnyOrder("ESTOQUE_MINIMO","LOTE_VENCIDO");
  });
  var alertas=json.readTree(mvc.perform(get("/api/v1/alertas").with(usuario(c.org()))).andReturn().getResponse().getContentAsString());
  var alerta=alertas.get(0);var dispensa=Map.of("chave",alerta.get("chave").asText(),"revisao",alerta.get("revisao").asText());
  mvc.perform(post("/api/v1/alertas/dispensar").with(usuario(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dispensa))).andExpect(status().isNotFound());
  mvc.perform(post("/api/v1/alertas/dispensar").with(usuario(c.org())).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dispensa))).andExpect(status().isOk());
  mvc.perform(get("/api/v1/alertas").with(usuario(c.org()))).andExpect(jsonPath("$.length()").value(1));
  mvc.perform(get("/api/v1/dashboard").with(usuario(c.org()))).andExpect(jsonPath("$.itens_ativos").value(1));
  mvc.perform(get("/api/v1/dashboard").with(usuario(UUID.randomUUID()))).andExpect(jsonPath("$.alertas_ativos").value(0));
 }
}
