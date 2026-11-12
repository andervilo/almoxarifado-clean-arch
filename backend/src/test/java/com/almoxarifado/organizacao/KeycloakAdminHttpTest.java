package com.almoxarifado.organizacao;
import static org.assertj.core.api.Assertions.*;
import java.util.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import com.almoxarifado.organizacao.domain.*;
import com.almoxarifado.organizacao.infrastructure.identity.*;
import com.almoxarifado.shared.domain.*;
class KeycloakAdminHttpTest {
 private void verificar(boolean outraOrganizacao,boolean protegido) throws Exception {
  UUID org=UUID.randomUUID(),id=UUID.randomUUID();var mutacoes=new AtomicInteger();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/",exchange->{
   String path=exchange.getRequestURI().getPath(),body;
   if(exchange.getRequestMethod().equals("POST") && path.endsWith("/token")) body="{\"access_token\":\"teste\"}";
   else if(exchange.getRequestMethod().equals("GET") && path.endsWith("/users/"+id)) body="{\"id\":\""+id+"\",\"attributes\":{\"organizacao_id\":[\""+(outraOrganizacao?UUID.randomUUID():org)+"\"]}}";
   else if(exchange.getRequestMethod().equals("GET") && path.endsWith("/realm/composite")) body=protegido?"[{\"name\":\"ADMIN_PLATAFORMA\"}]":"[]";
   else {mutacoes.incrementAndGet();body="{}";}
   byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
  });server.start();
  try {
   var props=new KeycloakAdminProperties(true,URI.create("http://127.0.0.1:"+server.getAddress().getPort()),"almoxarifado","cliente","segredo-nao-exposto");
   var adapter=new KeycloakAdminHttp(props,new ObjectMapper());
   assertThatThrownBy(()->adapter.preparar(org,UUID.randomUUID(),new DadosConta(id,"usuario","Usuario Teste","u@teste.local",PerfilUsuario.GESTOR,true,null)))
    .isInstanceOfSatisfying(RegraNegocioException.class,ex->assertThat(ex.getCodigo()).isEqualTo(outraOrganizacao?"CONTA_FORA_DA_ORGANIZACAO":"CONTA_PROTEGIDA"));
   assertThat(mutacoes).hasValue(0);assertThat(props.toString()).doesNotContain("segredo-nao-exposto");
  } finally {server.stop(0);}
 }
 @Test void naoAlteraContaDeOutroTenant() throws Exception {verificar(true,false);}
 @Test void naoAlteraAdministradorGlobal() throws Exception {verificar(false,true);}
}
