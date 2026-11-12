package com.almoxarifado;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
class ParametrosReposicaoIntegrationTest extends SuporteOperacional {
 private String almox(Cenario c,String local) {return jdbc.queryForObject("SELECT almoxarifado_id::text FROM estoque.locais_projecao WHERE organizacao_id=? AND id=?",String.class,c.org(),UUID.fromString(local));}
 private RequestPostProcessor escopo(Cenario c,String almox) {return jwt().jwt(j->j.subject("almoxarife").claim("organizacao_id",c.org().toString()).claim("almoxarifados",List.of(almox))).authorities(new SimpleGrantedAuthority("ROLE_ALMOXARIFE"));}
 private String rota(Cenario c,String almox) {return "/api/v1/catalogo/itens/"+c.item()+"/parametros-reposicao/"+almox;}
 @Test void limitesPorAlmoxarifadoGeramAlertasIsoladosPorEscopo() throws Exception {
  var c=cenario("NENHUM");String origem=almox(c,c.origem()),destino=almox(c,c.destino());
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->{
   assertThat(jdbc.queryForObject("SELECT count(*) FROM catalogo.almoxarifados_projecao WHERE organizacao_id=?",Integer.class,c.org())).isEqualTo(2);
   assertThat(jdbc.queryForObject("SELECT count(*) FROM alerta.almoxarifados_projecao WHERE organizacao_id=?",Integer.class,c.org())).isEqualTo(2);
  });
  assertThat(movimento(c,"entradas",UUID.randomUUID(),comando(c,10))).isEqualTo(201);
  mvc.perform(put(rota(c,origem)).with(usuario(c.org())).contentType(MediaType.APPLICATION_JSON).content("{\"minimo\":3,\"maximo\":5}")).andExpect(status().isOk());
  mvc.perform(put(rota(c,destino)).with(usuario(c.org())).contentType(MediaType.APPLICATION_JSON).content("{\"minimo\":1,\"maximo\":2}")).andExpect(status().isOk());
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->{
   mvc.perform(get("/api/v1/alertas").with(escopo(c,origem))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
    .andExpect(jsonPath("$[0].tipo").value("ESTOQUE_MAXIMO")).andExpect(jsonPath("$[0].almoxarifadoId").value(origem));
   mvc.perform(get("/api/v1/alertas").with(escopo(c,destino))).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].tipo").value("ESTOQUE_MINIMO")).andExpect(jsonPath("$[0].quantidade").value(0));
  });
  mvc.perform(get("/api/v1/alertas").with(usuario(c.org()))).andExpect(jsonPath("$.length()").value(2));
  mvc.perform(put(rota(c,origem)).with(escopo(c,destino)).contentType(MediaType.APPLICATION_JSON).content("{\"minimo\":3,\"maximo\":5}")).andExpect(status().isForbidden());
  var outro=cenario("NENHUM");
  mvc.perform(get(rota(c,origem)).with(usuario(outro.org()))).andExpect(status().isNotFound());
  mvc.perform(put(rota(c,origem)).with(usuario(c.org())).contentType(MediaType.APPLICATION_JSON).content("{\"minimo\":6,\"maximo\":5}")).andExpect(status().isBadRequest());
 }
 @Test void republicaParametrosExistentesSemDuplicarCadastro() throws Exception {
  var c=cenario("NENHUM");String almox=almox(c,c.origem());
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT count(*) FROM catalogo.almoxarifados_projecao WHERE organizacao_id=?",Integer.class,c.org())).isEqualTo(2));
  mvc.perform(put(rota(c,almox)).with(usuario(c.org())).contentType(MediaType.APPLICATION_JSON).content("{\"minimo\":3,\"maximo\":5}")).andExpect(status().isOk());
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT count(*) FROM alerta.parametros_reposicao WHERE organizacao_id=?",Integer.class,c.org())).isEqualTo(1));
  jdbc.update("DELETE FROM alerta.parametros_reposicao WHERE organizacao_id=?",c.org());
  mvc.perform(post("/api/v1/catalogo/sincronizacoes/parametros-reposicao").with(usuario(c.org()))).andExpect(jsonPath("$.registros").value(1));
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT minimo FROM alerta.parametros_reposicao WHERE organizacao_id=?",java.math.BigDecimal.class,c.org())).isEqualByComparingTo("3"));
  assertThat(jdbc.queryForObject("SELECT count(*) FROM catalogo.parametros_reposicao WHERE organizacao_id=?",Integer.class,c.org())).isEqualTo(1);
 }
}
