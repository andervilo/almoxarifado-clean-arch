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

class RequisicaoFluxoIntegrationTest extends SuporteOperacional {
 @Test void editaSomenteAntesDaReserva() throws Exception {
  var c=cenario("NENHUM");assertThat(movimento(c,"entradas",UUID.randomUUID(),comando(c,10))).isEqualTo(201);
  var body=Map.of("localId",c.origem(),"linhas",List.of(Map.of("itemId",c.item(),"quantidade",4)));
  var req=criar(c.org(),"/requisicoes",body);
  mvc.perform(put("/api/v1/requisicoes/"+req).with(usuario(c.org())).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
   .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RASCUNHO"));
  acao(c,req,"enviar");aguardar(c,req,"RESERVADA");
  mvc.perform(put("/api/v1/requisicoes/"+req).with(usuario(c.org())).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().isUnprocessableEntity());
 }
 private void acao(Cenario c,String req,String acao) throws Exception {
  mvc.perform(post("/api/v1/requisicoes/"+req+"/"+acao).with(usuario(c.org()))).andExpect(status().isAccepted());
 }
 private void aguardar(Cenario c,String req,String estado) {
  await().atMost(Duration.ofSeconds(15)).untilAsserted(()->assertThat(jdbc.queryForObject(
   "SELECT status FROM requisicao.requisicoes WHERE organizacao_id=? AND id=?",String.class,c.org(),UUID.fromString(req))).isEqualTo(estado));
 }
 @Test void reservaEntregaEConfirmacaoAtualizamSaldo() throws Exception {
  var c=cenario("NENHUM"); assertThat(movimento(c,"entradas",UUID.randomUUID(),comando(c,10))).isEqualTo(201);
  var req=criar(c.org(),"/requisicoes",Map.of("localId",c.origem(),"linhas",List.of(Map.of("itemId",c.item(),"quantidade",4))));
  acao(c,req,"enviar"); aguardar(c,req,"RESERVADA");
  assertThat(jdbc.queryForObject("SELECT reservado FROM estoque.posicoes WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("4");
  assertThat(movimento(c,"saidas",UUID.randomUUID(),comando(c,7))).isEqualTo(422);
  acao(c,req,"separar");acao(c,req,"entregar");aguardar(c,req,"ENTREGUE");acao(c,req,"confirmar-recebimento");
  assertThat(jdbc.queryForObject("SELECT quantidade FROM estoque.posicoes WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("6");
  assertThat(jdbc.queryForObject("SELECT reservado FROM estoque.posicoes WHERE organizacao_id=?",BigDecimal.class,c.org())).isZero();
  mvc.perform(post("/api/v1/requisicoes/"+req+"/entregar").with(usuario(c.org()))).andExpect(status().isUnprocessableEntity());
  mvc.perform(get("/api/v1/requisicoes/"+req).with(usuario(UUID.randomUUID()))).andExpect(status().isNotFound());
 }
 @Test void falhaEmUmaLinhaDesfazTodasAsReservasECancelamentoLiberaSaldo() throws Exception {
  var c=cenario("NENHUM");assertThat(movimento(c,"entradas",UUID.randomUUID(),comando(c,10))).isEqualTo(201);
  var req=criar(c.org(),"/requisicoes",Map.of("localId",c.origem(),"linhas",List.of(
   Map.of("itemId",c.item(),"quantidade",4),Map.of("itemId",UUID.fromString("7fffffff-ffff-ffff-7fff-ffffffffffff"),"quantidade",1))));
  acao(c,req,"enviar");aguardar(c,req,"FALHA_RESERVA");
  assertThat(jdbc.queryForObject("SELECT reservado FROM estoque.posicoes WHERE organizacao_id=?",BigDecimal.class,c.org())).isZero();
  var valida=criar(c.org(),"/requisicoes",Map.of("localId",c.origem(),"linhas",List.of(Map.of("itemId",c.item(),"quantidade",4))));
  acao(c,valida,"enviar");aguardar(c,valida,"RESERVADA");acao(c,valida,"cancelar");aguardar(c,valida,"CANCELADA");
  assertThat(jdbc.queryForObject("SELECT reservado FROM estoque.posicoes WHERE organizacao_id=?",BigDecimal.class,c.org())).isZero();
  assertThat(jdbc.queryForObject("SELECT quantidade FROM estoque.posicoes WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("10");
 }
}
