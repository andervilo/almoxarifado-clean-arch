package com.almoxarifado;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import java.util.*;
import java.time.Duration;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
class RepublicacaoIntegrationTest extends SuporteOperacional {
 @Test void reconstroiProjecoesSemMovimentarSaldoEComIsolamento() throws Exception {
  var c=cenario("NENHUM");assertThat(movimento(c,"entradas",UUID.randomUUID(),comando(c,10))).isEqualTo(201);
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT count(*) FROM inventario.saldos_projecao WHERE organizacao_id=?",Integer.class,c.org())).isEqualTo(1));
  jdbc.update("DELETE FROM inventario.saldos_projecao WHERE organizacao_id=?",c.org());
  jdbc.update("DELETE FROM alerta.saldos_projecao WHERE organizacao_id=?",c.org());
  jdbc.update("DELETE FROM estoque.itens_projecao WHERE organizacao_id=?",c.org());
  mvc.perform(post("/api/v1/catalogo/sincronizacoes/itens").with(usuario(c.org())))
   .andExpect(status().isOk()).andExpect(jsonPath("$.registros").value(1));
  mvc.perform(post("/api/v1/estoque/sincronizacoes/saldos").with(usuario(c.org())))
   .andExpect(status().isOk()).andExpect(jsonPath("$.registros").value(1));
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->{
   assertThat(jdbc.queryForObject("SELECT quantidade FROM inventario.saldos_projecao WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("10");
   assertThat(jdbc.queryForObject("SELECT quantidade FROM alerta.saldos_projecao WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("10");
   assertThat(jdbc.queryForObject("SELECT count(*) FROM estoque.itens_projecao WHERE organizacao_id=?",Integer.class,c.org())).isEqualTo(1);
  });
  assertThat(jdbc.queryForObject("SELECT count(*) FROM estoque.operacoes WHERE organizacao_id=?",Integer.class,c.org())).isEqualTo(1);
  assertThat(jdbc.queryForObject("SELECT quantidade FROM estoque.posicoes WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("10");
  mvc.perform(post("/api/v1/estoque/sincronizacoes/saldos").with(usuario(UUID.randomUUID())))
   .andExpect(jsonPath("$.registros").value(0));
 }
 @Test void exigeAdministradorEValidaPaginacao() throws Exception {
  var c=cenario("NENHUM");
  mvc.perform(post("/api/v1/estrutura/sincronizacoes/locais").with(jwt().jwt(j->j.claim("organizacao_id",c.org().toString())).authorities(new SimpleGrantedAuthority("ROLE_ALMOXARIFE"))))
   .andExpect(status().isForbidden());
  var primeira=json.readTree(mvc.perform(post("/api/v1/estrutura/sincronizacoes/locais?tamanho=1").with(usuario(c.org())))
   .andExpect(jsonPath("$.registros").value(1)).andExpect(jsonPath("$.concluido").value(false)).andReturn().getResponse().getContentAsString());
  mvc.perform(post("/api/v1/estrutura/sincronizacoes/locais?tamanho=1&apos="+primeira.get("proximoCursor").asText()).with(usuario(c.org())))
   .andExpect(jsonPath("$.registros").value(1));
  mvc.perform(post("/api/v1/estrutura/sincronizacoes/locais?tamanho=101").with(usuario(c.org()))).andExpect(status().isBadRequest());
 }
}
