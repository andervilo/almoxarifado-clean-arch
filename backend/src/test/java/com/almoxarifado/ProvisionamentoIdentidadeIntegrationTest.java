package com.almoxarifado;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import java.util.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.almoxarifado.organizacao.application.ProvedorIdentidade;
import com.almoxarifado.shared.domain.*;
class ProvisionamentoIdentidadeIntegrationTest extends SuporteOperacional {
 @org.springframework.beans.factory.annotation.Autowired com.almoxarifado.organizacao.application.SincronizarIdentidade sincronizar;
 @MockitoBean ProvedorIdentidade provedor;
 private Map<String,Object> dados() {return Map.of("username","novo.usuario","nome","Usuario Teste","email","novo@empresa.local","perfil","GESTOR","ativo",true,"almoxarifados",List.of());}
 private void estado(Cenario c,UUID id,String estado) {await().atMost(Duration.ofSeconds(10)).untilAsserted(()->assertThat(jdbc.queryForObject("SELECT status FROM organizacao.provisionamentos_identidade WHERE organizacao_id=? AND id=?",String.class,c.org(),id)).isEqualTo(estado));}
 @Test void ativacaoAntigaNaoReverteConfiguracaoMaisRecente() throws Exception {
  var c=cenario("NENHUM");var externo=UUID.randomUUID();var antigo=UUID.randomUUID();var recente=UUID.randomUUID();
  var dados=new com.almoxarifado.organizacao.domain.DadosConta(externo,"usuario.teste","Usuario Teste","u@empresa.local",com.almoxarifado.organizacao.domain.PerfilUsuario.GESTOR,true,null);
  for(var id:List.of(antigo,recente)) jdbc.update("INSERT INTO organizacao.provisionamentos_identidade(organizacao_id,id,ator,dados,status,keycloak_id) VALUES(?,?,'operador',?,'ATIVANDO',?)",c.org(),id,json.writeValueAsString(dados),externo);
  jdbc.update("INSERT INTO organizacao.identidades_gerenciadas(organizacao_id,keycloak_id,ultimo_provisionamento) VALUES(?,?,?)",c.org(),externo,recente);
  sincronizar.executar(c.org(),antigo);estado(c,antigo,"SUPERADO");verifyNoInteractions(provedor);
 }
 @Test void preparaDesabilitadoPersistePerfilEAtivaDepoisDoCommitSemDuplicar() throws Exception {
  var c=cenario("NENHUM");UUID id=UUID.randomUUID(),externo=UUID.randomUUID();when(provedor.habilitado()).thenReturn(true);when(provedor.preparar(any(),any(),any())).thenReturn(externo);
  doAnswer(inv->{assertThat(jdbc.queryForObject("SELECT count(*) FROM organizacao.usuarios WHERE organizacao_id=? AND codigo=?",Integer.class,c.org(),externo.toString())).isEqualTo(1);return null;}).when(provedor).definirAtivo(any(),any(),eq(true));
  mvc.perform(post("/api/v1/usuarios/provisionamentos").with(usuario(c.org())).header("Idempotency-Key",id).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados()))).andExpect(status().isAccepted());
  estado(c,id,"CONCLUIDO");
  mvc.perform(post("/api/v1/usuarios/provisionamentos").with(usuario(c.org())).header("Idempotency-Key",id).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados()))).andExpect(jsonPath("$.status").value("CONCLUIDO"));
  verify(provedor,times(1)).preparar(eq(c.org()),eq(id),any());verify(provedor,times(1)).definirAtivo(c.org(),externo,true);
  mvc.perform(get("/api/v1/usuarios/provisionamentos/"+id).with(usuario(UUID.randomUUID()))).andExpect(status().isNotFound());
 }
 @Test void falhaDeInfraestruturaPodeSerReprocessadaSemLiberarContaAntesDoCadastro() throws Exception {
  var c=cenario("NENHUM");UUID id=UUID.randomUUID(),externo=UUID.randomUUID();when(provedor.habilitado()).thenReturn(true);when(provedor.preparar(any(),any(),any())).thenThrow(new IntegracaoIndisponivelException("Indisponivel"));
  mvc.perform(post("/api/v1/usuarios/provisionamentos").with(usuario(c.org())).header("Idempotency-Key",id).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados()))).andExpect(status().isAccepted());
  await().atMost(Duration.ofSeconds(10)).untilAsserted(()->verify(provedor,atLeastOnce()).preparar(eq(c.org()),eq(id),any()));
  doReturn(externo).when(provedor).preparar(any(),any(),any());
  mvc.perform(post("/api/v1/usuarios/provisionamentos/"+id+"/reprocessar").with(usuario(c.org()))).andExpect(status().isAccepted());estado(c,id,"CONCLUIDO");
  verify(provedor,times(1)).definirAtivo(c.org(),externo,true);
 }
 @Test void bloqueiaPerfisGlobaisPapeisSemPermissaoEIntegracaoDesabilitada() throws Exception {
  var c=cenario("NENHUM");when(provedor.habilitado()).thenReturn(false);
  mvc.perform(post("/api/v1/usuarios/provisionamentos").with(usuario(c.org())).header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados()))).andExpect(status().isServiceUnavailable());
  when(provedor.habilitado()).thenReturn(true);var global=new HashMap<>(dados());global.put("perfil","ADMIN_PLATAFORMA");
  mvc.perform(post("/api/v1/usuarios/provisionamentos").with(usuario(c.org())).header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(global))).andExpect(status().isUnprocessableEntity());
  mvc.perform(post("/api/v1/usuarios/provisionamentos").with(jwt().jwt(j->j.claim("organizacao_id",c.org().toString())).authorities(new SimpleGrantedAuthority("ROLE_ALMOXARIFE"))).header("Idempotency-Key",UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(dados()))).andExpect(status().isForbidden());
 }
}
