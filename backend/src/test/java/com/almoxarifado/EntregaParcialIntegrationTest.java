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
class EntregaParcialIntegrationTest extends SuporteOperacional {
 private String pedido(Cenario c) throws Exception {
  assertThat(movimento(c,"entradas",UUID.randomUUID(),comando(c,10))).isEqualTo(201);
  var req=criar(c.org(),"/requisicoes",Map.of("localId",c.origem(),"linhas",List.of(Map.of("itemId",c.item(),"quantidade",6))));
  acao(c,req,"enviar");estado(c,req,"RESERVADA");acao(c,req,"separar");return req;
 }
 private void acao(Cenario c,String req,String acao) throws Exception {mvc.perform(post("/api/v1/requisicoes/"+req+"/"+acao).with(usuario(c.org()))).andExpect(status().isAccepted());}
 private void estado(Cenario c,String req,String estado) {
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT status FROM requisicao.requisicoes WHERE organizacao_id=? AND id=?",String.class,c.org(),UUID.fromString(req))).isEqualTo(estado));
 }
 private void parcial(Cenario c,String req,UUID chave,int quantidade,int esperado) throws Exception {
  mvc.perform(post("/api/v1/requisicoes/"+req+"/entregar").with(usuario(c.org())).header("Idempotency-Key",chave).contentType(MediaType.APPLICATION_JSON)
   .content(json.writeValueAsString(Map.of("linhas",List.of(Map.of("itemId",c.item(),"quantidade",quantidade)))))).andExpect(status().is(esperado));
 }
 @Test void entregaEmDuasEtapasSemDuplicarRepeticaoDaChave() throws Exception {
  var c=cenario("NENHUM");var req=pedido(c);var chave=UUID.randomUUID();
  parcial(c,req,chave,2,202);estado(c,req,"PARCIALMENTE_ENTREGUE");
  parcial(c,req,chave,2,202);parcial(c,req,chave,3,422);
  assertThat(jdbc.queryForObject("SELECT atendida FROM requisicao.linhas WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("2");
  assertThat(jdbc.queryForObject("SELECT reservado FROM estoque.posicoes WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("4");
  parcial(c,req,UUID.randomUUID(),5,422);
  parcial(c,req,UUID.randomUUID(),4,202);estado(c,req,"ENTREGUE");acao(c,req,"confirmar-recebimento");
  assertThat(jdbc.queryForObject("SELECT quantidade FROM estoque.posicoes WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("4");
  assertThat(jdbc.queryForObject("SELECT reservado FROM estoque.posicoes WHERE organizacao_id=?",BigDecimal.class,c.org())).isZero();
  mvc.perform(get("/api/v1/requisicoes/"+req).with(usuario(c.org()))).andExpect(jsonPath("$.linhas[0].atendida").value(6)).andExpect(jsonPath("$.linhas[0].pendente").value(0));
 }
 @Test void cancelarParcialLiberaSomenteRestanteEPreservaEntregue() throws Exception {
  var c=cenario("NENHUM");var req=pedido(c);parcial(c,req,UUID.randomUUID(),2,202);estado(c,req,"PARCIALMENTE_ENTREGUE");
  acao(c,req,"cancelar");estado(c,req,"CANCELADA");
  assertThat(jdbc.queryForObject("SELECT quantidade FROM estoque.posicoes WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("8");
  assertThat(jdbc.queryForObject("SELECT reservado FROM estoque.posicoes WHERE organizacao_id=?",BigDecimal.class,c.org())).isZero();
  assertThat(jdbc.queryForObject("SELECT atendida FROM requisicao.linhas WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("2");
 }
}
