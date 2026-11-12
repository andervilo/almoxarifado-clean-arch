package com.almoxarifado;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
class EscopoAlmoxarifadoIntegrationTest extends SuporteOperacional {
 private RequestPostProcessor escopo(UUID org,List<String> ids) {
  return jwt().jwt(j->j.subject("almoxarife").claim("organizacao_id",org.toString()).claim("almoxarifados",ids))
   .authorities(new SimpleGrantedAuthority("ROLE_ALMOXARIFE"));
 }
 private String almox(Cenario c,String local) {
  return jdbc.queryForObject("SELECT almoxarifado_id::text FROM estoque.locais_projecao WHERE organizacao_id=? AND id=?",String.class,c.org(),UUID.fromString(local));
 }
 @Test void limitaEscritaConsultaKardexECustoEFiltraAntesDaPaginacao() throws Exception {
  var c=cenario("NENHUM");assertThat(movimento(c,"entradas",UUID.randomUUID(),comando(c,10))).isEqualTo(201);
  var entrada=comando(c,2);entrada.put("destinoId",c.destino());assertThat(movimento(c,"entradas",UUID.randomUUID(),entrada)).isEqualTo(201);
  var permitidos=List.of(almox(c,c.destino()));
  mvc.perform(get("/api/v1/almoxarifados?tamanho=1").with(escopo(c.org(),permitidos))).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(permitidos.getFirst()));
  mvc.perform(get("/api/v1/almoxarifados/"+almox(c,c.origem())).with(escopo(c.org(),permitidos))).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/estoque/saldos?itemId="+c.item()).with(escopo(c.org(),permitidos))).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].localId").value(c.destino()));
  mvc.perform(get("/api/v1/estoque/kardex?itemId="+c.item()).with(escopo(c.org(),permitidos))).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].local_id").value(c.destino()));
  mvc.perform(get("/api/v1/estoque/custos?itemId="+c.item()).with(escopo(c.org(),permitidos))).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].quantidade").value(2));
  var trans=comando(c,1);trans.put("destinoId",c.destino());
  mvc.perform(post("/api/v1/estoque/movimentacoes/transferencias").with(escopo(c.org(),permitidos)).header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(trans))).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/estoque/movimentacoes?itemId="+c.item()).with(escopo(c.org(),List.of()))).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(get("/api/v1/estoque/saldos?itemId="+c.item()).with(escopo(c.org(),List.of()))).andExpect(jsonPath("$.length()").value(0));
 }
 @Test void protegePedidosInventariosEAlertas() throws Exception {
  var c=cenario("LOTE_VALIDADE");var entrada=comando(c,2);entrada.put("lote","L");entrada.put("validade",LocalDate.now(ZoneOffset.UTC).minusDays(1).toString());
  assertThat(movimento(c,"entradas",UUID.randomUUID(),entrada)).isEqualTo(201);
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->{
   for(String modulo:List.of("requisicao","inventario","alerta")) assertThat(jdbc.queryForObject("SELECT count(*) FROM "+modulo+".locais_projecao WHERE organizacao_id=?",Integer.class,c.org())).isEqualTo(2);
   assertThat(jdbc.queryForObject("SELECT count(*) FROM inventario.saldos_projecao WHERE organizacao_id=?",Integer.class,c.org())).isEqualTo(1);
   assertThat(jdbc.queryForObject("SELECT count(*) FROM alerta.saldos_projecao WHERE organizacao_id=?",Integer.class,c.org())).isEqualTo(1);
  });
  var req=criar(c.org(),"/requisicoes",Map.of("localId",c.origem(),"linhas",List.of(Map.of("itemId",c.item(),"quantidade",1))));
  var inv=criar(c.org(),"/inventarios",Map.of("localId",c.origem()));var fora=List.of(almox(c,c.destino()));
  mvc.perform(get("/api/v1/requisicoes/"+req).with(escopo(c.org(),fora))).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/requisicoes").with(escopo(c.org(),fora))).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(get("/api/v1/inventarios/"+inv).with(escopo(c.org(),fora))).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/inventarios").with(escopo(c.org(),fora))).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(post("/api/v1/inventarios").with(escopo(c.org(),fora)).contentType(MediaType.APPLICATION_JSON).content("{\"localId\":\""+c.origem()+"\"}")).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/alertas").with(escopo(c.org(),fora))).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(get("/api/v1/dashboard").with(escopo(c.org(),fora))).andExpect(jsonPath("$.alertas_ativos").value(0));
  mvc.perform(get("/api/v1/alertas").with(escopo(c.org(),List.of(almox(c,c.origem()))))).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].tipo").value("LOTE_VENCIDO"));
 }
 @Test void escopoMalformadoNaoViraAcessoIrrestrito() throws Exception {
  var c=cenario("NENHUM");
  mvc.perform(get("/api/v1/almoxarifados").with(escopo(c.org(),List.of("invalido")))).andExpect(status().isForbidden());
  mvc.perform(get("/api/v1/estoque/saldos?itemId="+c.item()).with(escopo(c.org(),List.of("1-1-1-1-1")))).andExpect(status().isForbidden());
 }
}
