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
class EstoqueApiIntegrationTest extends SuporteOperacional {
 @Test void falhaNaGravacaoDoDestinoDesfazSaidaECustoDaOrigem() throws Exception {
  var c=cenario("NENHUM");assertThat(movimento(c,"entradas",UUID.randomUUID(),comando(c,10))).isEqualTo(201);
  String constraint="ck_destino_"+UUID.randomUUID().toString().replace("-","");
  jdbc.execute("ALTER TABLE estoque.posicoes ADD CONSTRAINT "+constraint+" CHECK(local_id <> '"+c.destino()+"'::uuid)");
  try {
   var transferir=comando(c,2);transferir.put("destinoId",c.destino());
   assertThat(movimento(c,"transferencias",UUID.randomUUID(),transferir)).isEqualTo(409);
   assertThat(jdbc.queryForObject("SELECT quantidade FROM estoque.posicoes WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("10");
   assertThat(jdbc.queryForObject("SELECT quantidade FROM estoque.custos WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("10");
   assertThat(jdbc.queryForObject("SELECT count(*) FROM estoque.operacoes WHERE organizacao_id=?",Integer.class,c.org())).isEqualTo(1);
  } finally { jdbc.execute("ALTER TABLE estoque.posicoes DROP CONSTRAINT "+constraint); }
 }
 @Test void serieEhUnicaERastreavelAposTransferencia() throws Exception {
  var c=cenario("NUMERO_SERIE");var entrada=comando(c,1);entrada.put("serie","S-1");
  assertThat(movimento(c,"entradas",UUID.randomUUID(),entrada)).isEqualTo(201);
  assertThat(movimento(c,"entradas",UUID.randomUUID(),entrada)).isEqualTo(422);
  var trans=comando(c,1);trans.put("serie","S-1");trans.put("destinoId",c.destino());
  assertThat(movimento(c,"transferencias",UUID.randomUUID(),trans)).isEqualTo(201);
  mvc.perform(get("/api/v1/estoque/kardex").param("itemId",c.item()).param("serie","S-1").with(usuario(c.org())))
   .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
 }
 @Test void fifoEBloqueioDeItemInativo() throws Exception {
  var c=cenario("LOTE");
  for(String lote:List.of("PRIMEIRO","SEGUNDO")) { var e=comando(c,2);e.put("lote",lote);assertThat(movimento(c,"entradas",UUID.randomUUID(),e)).isEqualTo(201); }
  assertThat(movimento(c,"saidas",UUID.randomUUID(),comando(c,1))).isEqualTo(201);
  assertThat(jdbc.queryForObject("SELECT quantidade FROM estoque.posicoes WHERE organizacao_id=? AND lote='PRIMEIRO'",BigDecimal.class,c.org())).isEqualByComparingTo("1");
  var item=json.readTree(mvc.perform(get("/api/v1/catalogo/itens/"+c.item()).with(usuario(c.org()))).andReturn().getResponse().getContentAsString());
  ((com.fasterxml.jackson.databind.node.ObjectNode)item).put("ativo",false);
  mvc.perform(put("/api/v1/catalogo/itens/"+c.item()).with(usuario(c.org())).contentType(MediaType.APPLICATION_JSON).content(item.toString())).andExpect(status().isOk());
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT ativo FROM estoque.itens_projecao WHERE organizacao_id=?",Boolean.class,c.org())).isFalse());
  assertThat(movimento(c,"saidas",UUID.randomUUID(),comando(c,1))).isEqualTo(422);
 }
 @Test void entradaIdempotenteSaidaTransferenciaEEstornoSaoAtomicos() throws Exception {
  var c=cenario("NENHUM"); UUID entrada=UUID.randomUUID();
  assertThat(movimento(c,"entradas",entrada,comando(c,10))).isEqualTo(201);
  assertThat(movimento(c,"entradas",entrada,comando(c,10))).isEqualTo(201);
  assertThat(movimento(c,"entradas",entrada,comando(c,11))).isEqualTo(422);
  assertThat(movimento(c,"saidas",UUID.randomUUID(),comando(c,4))).isEqualTo(201);
  assertThat(movimento(c,"saidas",UUID.randomUUID(),comando(c,7))).isEqualTo(422);
  var trans= comando(c,2); trans.put("destinoId",c.destino()); UUID transferencia=UUID.randomUUID();
  assertThat(movimento(c,"transferencias",transferencia,trans)).isEqualTo(201);
  var estorno=comando(c,2);estorno.put("referenciaId",transferencia);
  assertThat(movimento(c,"estornos",UUID.randomUUID(),estorno)).isEqualTo(201);
  assertThat(movimento(c,"estornos",UUID.randomUUID(),estorno)).isEqualTo(422);
  assertThat(jdbc.queryForObject("SELECT sum(quantidade) FROM estoque.posicoes WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("6");
  assertThat(jdbc.queryForObject("SELECT quantidade FROM estoque.posicoes WHERE organizacao_id=? AND local_id=?",BigDecimal.class,c.org(),UUID.fromString(c.origem()))).isEqualByComparingTo("6");
  assertThat(jdbc.queryForObject("SELECT count(*) FROM estoque.operacoes WHERE organizacao_id=?",Integer.class,c.org())).isEqualTo(4);
  assertThatThrownBy(()->jdbc.update("DELETE FROM estoque.operacoes WHERE organizacao_id=?",c.org())).isInstanceOf(org.springframework.dao.DataAccessException.class);
 }
 @Test void escolheFEFOBloqueiaVencidosECalculaCustoMedio() throws Exception {
  var c=cenario("LOTE_VALIDADE");
  for(int dias : new int[]{30,10,-1}) {
   var body=comando(c,2);body.put("lote","L"+dias);body.put("validade",LocalDate.now(ZoneOffset.UTC).plusDays(dias).toString());body.put("custo",dias==30?2:5);
   assertThat(movimento(c,"entradas",UUID.randomUUID(),body)).isEqualTo(201);
  }
  assertThat(jdbc.queryForObject("SELECT custo FROM estoque.custos WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("4");
  assertThat(movimento(c,"saidas",UUID.randomUUID(),comando(c,3))).isEqualTo(201);
  assertThat(jdbc.queryForObject("SELECT quantidade FROM estoque.posicoes WHERE organizacao_id=? AND lote='L10'",BigDecimal.class,c.org())).isZero();
  assertThat(jdbc.queryForObject("SELECT quantidade FROM estoque.posicoes WHERE organizacao_id=? AND lote='L30'",BigDecimal.class,c.org())).isEqualByComparingTo("1");
  assertThat(movimento(c,"saidas",UUID.randomUUID(),comando(c,2))).isEqualTo(422);
 }
 @Test void concorrenciaNaoPermiteSaldoNegativo() throws Exception {
  var c=cenario("NENHUM"); assertThat(movimento(c,"entradas",UUID.randomUUID(),comando(c,5))).isEqualTo(201);
  try(var pool=Executors.newFixedThreadPool(2)) {
   var partida=new CountDownLatch(1);
   Callable<Integer> saida=()->{partida.await();return movimento(c,"saidas",UUID.randomUUID(),comando(c,4));};
   var a=pool.submit(saida); var b=pool.submit(saida); partida.countDown();
   assertThat(List.of(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,422);
  }
  assertThat(jdbc.queryForObject("SELECT sum(quantidade) FROM estoque.posicoes WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("1");
 }
}
