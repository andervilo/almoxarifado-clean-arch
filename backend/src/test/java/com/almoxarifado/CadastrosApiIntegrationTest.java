package com.almoxarifado;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.*;
import com.fasterxml.jackson.databind.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class CadastrosApiIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired JdbcTemplate jdbc;
 @Autowired ObjectMapper json;
 private RequestPostProcessor usuario(UUID org,String perfil) {
  return jwt().jwt(j->j.subject("operador").claim("organizacao_id",org.toString())).authorities(new SimpleGrantedAuthority("ROLE_"+perfil));
 }
 private JsonNode criar(UUID org,String rota,String body) throws Exception {
  return json.readTree(mvc.perform(post("/api/v1"+rota).with(usuario(org,"ADMIN_PLATAFORMA"))
   .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
 }
 private UUID organizacao() throws Exception {
  UUID org=UUID.randomUUID();
  criar(org,"/organizacoes","{\"id\":\""+org+"\",\"nome\":\"Empresa\",\"codigo\":\""+org+"\"}");
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->{
   assertThat(jdbc.queryForObject("SELECT count(*) FROM catalogo.organizacoes_projecao WHERE id=?",Integer.class,org)).isEqualTo(1);
   assertThat(jdbc.queryForObject("SELECT count(*) FROM estrutura.organizacoes_projecao WHERE id=?",Integer.class,org)).isEqualTo(1);
  });
  return org;
 }
 @Test void cadastraItemComReferenciasLocaisEImpedeAcessoEntreTenants() throws Exception {
  var a=organizacao(); var b=organizacao();
  var categoria=criar(a,"/catalogo/categorias","{\"codigo\":\"C\",\"nome\":\"Categoria\",\"ativo\":true}").get("id").asText();
  var unidade=criar(a,"/catalogo/unidades-medida","{\"codigo\":\"UN\",\"nome\":\"Unidade\",\"ativo\":true}").get("id").asText();
  String item="""
   {"codigo":"I","descricao":"Material","categoriaId":"%s","unidadeMedidaId":"%s",
    "rastreabilidade":"LOTE_VALIDADE","estoqueMinimo":1,"estoqueMaximo":100,"ativo":true}
   """.formatted(categoria,unidade);
  var id=criar(a,"/catalogo/itens",item).get("id").asText();
  mvc.perform(get("/api/v1/catalogo/itens/"+id).with(usuario(b,"GESTOR"))).andExpect(status().isNotFound());
  mvc.perform(post("/api/v1/catalogo/itens").with(usuario(b,"ALMOXARIFE")).contentType(MediaType.APPLICATION_JSON).content(item)).andExpect(status().isNotFound());
  mvc.perform(post("/api/v1/catalogo/itens").with(usuario(a,"REQUISITANTE")).contentType(MediaType.APPLICATION_JSON).content(item)).andExpect(status().isForbidden());
  mvc.perform(put("/api/v1/catalogo/itens/"+id).with(usuario(a,"ALMOXARIFE")).contentType(MediaType.APPLICATION_JSON)
    .content(item.replace("LOTE_VALIDADE","NENHUM"))).andExpect(status().isUnprocessableEntity());
  mvc.perform(get("/api/v1/catalogo/itens?tamanho=101").with(usuario(a,"GESTOR"))).andExpect(status().isBadRequest());
  assertThat(jdbc.queryForObject("SELECT count(*) FROM catalogo.itens WHERE organizacao_id=?",Integer.class,b)).isZero();
 }
 @Test void localPertenceAUmAlmoxarifadoDoMesmoTenant() throws Exception {
  var a=organizacao(); var b=organizacao();
  var almox=criar(a,"/almoxarifados","{\"codigo\":\"A\",\"nome\":\"Central\",\"ativo\":true}").get("id").asText();
  String local="{\"codigo\":\"L\",\"descricao\":\"Prateleira\",\"ativo\":true}";
  criar(a,"/almoxarifados/"+almox+"/locais",local);
  mvc.perform(post("/api/v1/almoxarifados/"+almox+"/locais").with(usuario(b,"ALMOXARIFE"))
    .contentType(MediaType.APPLICATION_JSON).content(local)).andExpect(status().isNotFound());
  mvc.perform(get("/api/v1/almoxarifados/"+almox+"/locais").with(usuario(a,"GESTOR")))
    .andExpect(jsonPath("$.length()").value(1));
 }
}
