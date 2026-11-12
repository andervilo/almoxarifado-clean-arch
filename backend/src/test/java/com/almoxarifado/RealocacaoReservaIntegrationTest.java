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
class RealocacaoReservaIntegrationTest extends SuporteOperacional {
 private void acao(Cenario c,String req,String acao) throws Exception {mvc.perform(post("/api/v1/requisicoes/"+req+"/"+acao).with(usuario(c.org()))).andExpect(status().isAccepted());}
 private void estado(Cenario c,String req,String estado) {await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT status FROM requisicao.requisicoes WHERE organizacao_id=? AND id=?",String.class,c.org(),UUID.fromString(req))).isEqualTo(estado));}
 private void alocada(Cenario c,String req,String lote,int q) {
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->mvc.perform(get("/api/v1/requisicoes/"+req+"/alocacoes").with(usuario(c.org())))
   .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].lote").value(lote)).andExpect(jsonPath("$[0].quantidade").value(q)));
 }
 private void trocar(Cenario c,String req,UUID chave,UUID pos,int q) throws Exception {
  mvc.perform(put("/api/v1/requisicoes/"+req+"/alocacoes/"+c.item()).with(usuario(c.org())).header("Idempotency-Key",chave).contentType(MediaType.APPLICATION_JSON)
   .content(json.writeValueAsString(Map.of("motivo","Selecao operacional","alocacoes",List.of(Map.of("posicaoId",pos,"quantidade",q)))))).andExpect(status().isAccepted());
 }
 @Test void trocaLotesAtomicamenteSuportaParcialEReconstroiProjecao() throws Exception {
  var c=cenario("LOTE");for(String lote:List.of("A","B","C")) {var e=comando(c,lote.equals("C")?1:5);e.put("lote",lote);assertThat(movimento(c,"entradas",UUID.randomUUID(),e)).isEqualTo(201);}
  var req=criar(c.org(),"/requisicoes",Map.of("localId",c.origem(),"linhas",List.of(Map.of("itemId",c.item(),"quantidade",4))));
  acao(c,req,"enviar");estado(c,req,"RESERVADA");alocada(c,req,"A",4);
  var posB=jdbc.queryForObject("SELECT id FROM estoque.posicoes WHERE organizacao_id=? AND lote='B'",UUID.class,c.org());
  UUID chave=UUID.randomUUID();trocar(c,req,chave,posB,4);estado(c,req,"RESERVADA");alocada(c,req,"B",4);
  int operacoes=jdbc.queryForObject("SELECT count(*) FROM estoque.operacoes WHERE organizacao_id=?",Integer.class,c.org());
  trocar(c,req,chave,posB,4);assertThat(jdbc.queryForObject("SELECT count(*) FROM estoque.operacoes WHERE organizacao_id=?",Integer.class,c.org())).isEqualTo(operacoes);
  var posC=jdbc.queryForObject("SELECT id FROM estoque.posicoes WHERE organizacao_id=? AND lote='C'",UUID.class,c.org());
  trocar(c,req,UUID.randomUUID(),posC,4);estado(c,req,"RESERVADA");
  assertThat(jdbc.queryForObject("SELECT erro FROM requisicao.requisicoes WHERE organizacao_id=? AND id=?",String.class,c.org(),UUID.fromString(req))).isEqualTo("SALDO_INSUFICIENTE");
  assertThat(jdbc.queryForObject("SELECT reservado FROM estoque.posicoes WHERE organizacao_id=? AND lote='B'",BigDecimal.class,c.org())).isEqualByComparingTo("4");
  acao(c,req,"separar");
  mvc.perform(post("/api/v1/requisicoes/"+req+"/entregar").with(usuario(c.org())).header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
   .content(json.writeValueAsString(Map.of("linhas",List.of(Map.of("itemId",c.item(),"quantidade",2)))))).andExpect(status().isAccepted());
  estado(c,req,"PARCIALMENTE_ENTREGUE");alocada(c,req,"B",2);
  jdbc.update("DELETE FROM requisicao.alocacoes WHERE organizacao_id=?",c.org());
  mvc.perform(post("/api/v1/estoque/sincronizacoes/reservas").with(usuario(c.org()))).andExpect(jsonPath("$.registros").value(1));
  alocada(c,req,"B",2);
  assertThat(jdbc.queryForObject("SELECT sum(quantidade) FROM estoque.posicoes WHERE organizacao_id=?",BigDecimal.class,c.org())).isEqualByComparingTo("9");
 }
}
