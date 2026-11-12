package com.almoxarifado;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
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

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Import(TestcontainersConfiguration.class)
abstract class SuporteOperacional {
 @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc; @Autowired ObjectMapper json;
 record Cenario(UUID org,String item,String origem,String destino) {}
 protected RequestPostProcessor usuario(UUID org) {
  return jwt().jwt(j->j.subject("operador").claim("organizacao_id",org.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN_PLATAFORMA"));
 }
 protected String criar(UUID org,String rota,Object body) throws Exception {
  return json.readTree(mvc.perform(post("/api/v1"+rota).with(usuario(org)).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
   .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asText();
 }
 protected Cenario cenario(String rastreio) throws Exception {
  UUID org=UUID.randomUUID(); criar(org,"/organizacoes",Map.of("id",org,"nome","Empresa","codigo",org.toString()));
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT count(*) FROM catalogo.organizacoes_projecao WHERE id=?",Integer.class,org)).isEqualTo(1));
  String cat=criar(org,"/catalogo/categorias",Map.of("codigo","C","nome","Categoria","ativo",true));
  String un=criar(org,"/catalogo/unidades-medida",Map.of("codigo","U","nome","Unidade","ativo",true));
  String item=criar(org,"/catalogo/itens",Map.of("codigo","I","descricao","Item","categoriaId",cat,"unidadeMedidaId",un,"rastreabilidade",rastreio,"estoqueMinimo",1,"estoqueMaximo",100,"ativo",true));
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT count(*) FROM estrutura.organizacoes_projecao WHERE id=?",Integer.class,org)).isEqualTo(1));
  String almox=criar(org,"/almoxarifados",Map.of("codigo","A","nome","Central","ativo",true));
  String almox2=criar(org,"/almoxarifados",Map.of("codigo","B","nome","Secundario","ativo",true));
  String origem=criar(org,"/almoxarifados/"+almox+"/locais",Map.of("codigo","L","descricao","Origem","ativo",true));
  String destino=criar(org,"/almoxarifados/"+almox2+"/locais",Map.of("codigo","L","descricao","Destino","ativo",true));
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->{
   assertThat(jdbc.queryForObject("SELECT count(*) FROM estoque.itens_projecao WHERE organizacao_id=?",Integer.class,org)).isEqualTo(1);
   assertThat(jdbc.queryForObject("SELECT count(*) FROM estoque.locais_projecao WHERE organizacao_id=?",Integer.class,org)).isEqualTo(2);
   assertThat(jdbc.queryForObject("SELECT count(*) FROM estoque.almoxarifados_projecao WHERE organizacao_id=?",Integer.class,org)).isEqualTo(2);
  }); return new Cenario(org,item,origem,destino);
 }
 protected Map<String,Object> comando(Cenario c,int quantidade) {
  var body=new HashMap<String,Object>(); body.put("itemId",c.item()); body.put("origemId",c.origem());body.put("destinoId",c.origem());
  body.put("quantidade",quantidade);body.put("custo",2);body.put("motivo","Teste operacional"); return body;
 }
 protected int movimento(Cenario c,String tipo,UUID id,Map<String,Object> body) throws Exception {
  return mvc.perform(post("/api/v1/estoque/movimentacoes/"+tipo).with(usuario(c.org())).header("Idempotency-Key",id)
   .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andReturn().getResponse().getStatus();
 }
}
