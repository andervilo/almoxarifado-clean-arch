package com.almoxarifado.organizacao.infrastructure.identity;
import java.util.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.io.IOException;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.almoxarifado.organizacao.application.ProvedorIdentidade;
import com.almoxarifado.organizacao.domain.*;
import com.almoxarifado.shared.domain.*;

@Component
public class KeycloakAdminHttp implements ProvedorIdentidade {
 private static final Set<String> PERFIS=Set.of("ADMIN_ORGANIZACAO","ALMOXARIFE","REQUISITANTE","GESTOR");
 private final KeycloakAdminProperties props;private final ObjectMapper json;private final HttpClient http;
 public KeycloakAdminHttp(KeycloakAdminProperties props,ObjectMapper json) {
  this.props=props;this.json=json;this.http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
 }
 public boolean habilitado() {return props.enabled() && props.clientSecret()!=null && !props.clientSecret().isBlank();}
 private String base() {return props.baseUrl().toString().replaceAll("/+$","");}
 private String admin() {return "/admin/realms/"+props.realm();}
 private String encode(String valor) {return URLEncoder.encode(valor,StandardCharsets.UTF_8);}
 private IntegracaoIndisponivelException indisponivel() {return new IntegracaoIndisponivelException("Keycloak administrativo indisponivel; reprocessamento preservado");}
 private RegraNegocioException regra(String codigo) {return new RegraNegocioException(codigo,"Operacao de identidade recusada pelo provedor");}
 private HttpResponse<String> enviar(String metodo,String caminho,String token,String body,String tipo) {
  var builder=HttpRequest.newBuilder(URI.create(base()+caminho)).timeout(Duration.ofSeconds(10)).header("Accept","application/json");
  if(token!=null) builder.header("Authorization","Bearer "+token);
  if(tipo!=null) builder.header("Content-Type",tipo);
  builder.method(metodo,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8));
  try {
   var resposta=http.send(builder.build(),HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
   if(resposta.statusCode()>=500 || resposta.statusCode()==401 || resposta.statusCode()==403 || resposta.statusCode()>=300 && resposta.statusCode()<400) throw indisponivel();
   return resposta;
  } catch(InterruptedException ex) {Thread.currentThread().interrupt();throw indisponivel();} catch(IOException ex) {throw indisponivel();}
 }
 private JsonNode ler(String texto) {try{return json.readTree(texto);}catch(IOException ex){throw indisponivel();}}
 private void exigir(HttpResponse<String> resposta,int... permitidos) {
  for(int codigo:permitidos) if(resposta.statusCode()==codigo) return;
  throw regra(switch(resposta.statusCode()){case 404->"CONTA_NAO_ENCONTRADA";case 409->"CONTA_EXISTENTE";case 400->"DADOS_IDENTIDADE_INVALIDOS";default->"PROVEDOR_RECUSOU_IDENTIDADE";});
 }
 private HttpResponse<String> admin(String metodo,String caminho,String token,JsonNode body) {
  return enviar(metodo,admin()+caminho,token,body==null?null:body.toString(),body==null?null:"application/json");
 }
 private String token() {
  if(!habilitado()) throw indisponivel();
  var resposta=enviar("POST","/realms/"+props.realm()+"/protocol/openid-connect/token",null,
   "grant_type=client_credentials&client_id="+encode(props.clientId())+"&client_secret="+encode(props.clientSecret()),"application/x-www-form-urlencoded");
  if(resposta.statusCode()!=200) throw indisponivel();
  String token=ler(resposta.body()).path("access_token").asText();if(token.isBlank()) throw indisponivel();return token;
 }
 private ObjectNode buscar(String token,UUID id) {
  var resposta=admin("GET","/users/"+id,token,null);exigir(resposta,200);
  var dado=ler(resposta.body());if(!(dado instanceof ObjectNode objeto)) throw indisponivel();return objeto;
 }
 private void tenant(UUID org,JsonNode usuario) {
  var valor=usuario.path("attributes").path("organizacao_id");
  if(!valor.isArray() || valor.size()!=1 || !valor.get(0).isTextual() || !org.toString().equalsIgnoreCase(valor.get(0).asText())) throw regra("CONTA_FORA_DA_ORGANIZACAO");
  if(!usuario.path("serviceAccountClientId").asText().isBlank()) throw regra("CONTA_PROTEGIDA");
 }
 private JsonNode papeis(String token,UUID id,boolean efetivos) {
  var resposta=admin("GET","/users/"+id+"/role-mappings/realm"+(efetivos?"/composite":""),token,null);exigir(resposta,200);return ler(resposta.body());
 }
 private void proteger(String token,UUID id) {
  for(var papel:papeis(token,id,true)) if(papel.path("name").asText().equals("ADMIN_PLATAFORMA")) throw regra("CONTA_PROTEGIDA");
  var resposta=admin("GET","/users/"+id+"/role-mappings",token,null);exigir(resposta,200);
  if(!ler(resposta.body()).path("clientMappings").path("realm-management").path("mappings").isEmpty()) throw regra("CONTA_PROTEGIDA");
 }
 public UUID preparar(UUID org,UUID pedido,DadosConta dados) {
  String token=token();UUID id=dados.keycloakId();ObjectNode usuario;
  if(id==null) {
   var lista=admin("GET","/users?username="+encode(dados.username())+"&exact=true",token,null);exigir(lista,200);var encontrados=ler(lista.body());
   if(encontrados.size()>1) throw regra("CONTA_EXISTENTE");
   if(encontrados.size()==1) {
    var existente=encontrados.get(0);tenant(org,existente);
    if(!pedido.toString().equals(existente.path("attributes").path("provisionamento_id").path(0).asText())) throw regra("CONTA_EXISTENTE");
    id=UUID.fromString(existente.path("id").asText());usuario=buscar(token,id);
   } else {
    usuario=json.createObjectNode();usuario.put("username",dados.username());usuario.put("enabled",false);
    var atributos=usuario.putObject("attributes");atributos.putArray("organizacao_id").add(org.toString());atributos.putArray("provisionamento_id").add(pedido.toString());
    usuario.put("email",dados.email());String[] nome=dados.nome().strip().split("\\s+",2);usuario.put("firstName",nome[0]);usuario.put("lastName",nome.length>1?nome[1]:nome[0]);usuario.putArray("requiredActions").add("UPDATE_PASSWORD");
    var criada=admin("POST","/users",token,usuario);exigir(criada,201);
    String local=criada.headers().firstValue("Location").orElseThrow(this::indisponivel);try{id=UUID.fromString(local.substring(local.lastIndexOf('/')+1));}catch(IllegalArgumentException ex){throw indisponivel();}
    usuario=buscar(token,id);
   }
  } else usuario=buscar(token,id);
  tenant(org,usuario);proteger(token,id);
  var diretos=papeis(token,id,false);Set<String> diretosApp=new HashSet<>();for(var papel:diretos) if(PERFIS.contains(papel.path("name").asText())) diretosApp.add(papel.path("name").asText());
  for(var papel:papeis(token,id,true)) {String nome=papel.path("name").asText();if(PERFIS.contains(nome) && !diretosApp.contains(nome) && !nome.equals(dados.perfil().name())) throw regra("PERFIL_HERDADO");}
  usuario.put("enabled",false);usuario.put("username",dados.username());usuario.put("email",dados.email());
  String[] nome=dados.nome().strip().split("\\s+",2);usuario.put("firstName",nome[0]);usuario.put("lastName",nome.length>1?nome[1]:nome[0]);
  var atributos=usuario.withObject("/attributes");atributos.putArray("organizacao_id").add(org.toString());atributos.putArray("escopo_restrito").add(Boolean.toString(dados.almoxarifados()!=null));
  if(dados.almoxarifados()==null || dados.almoxarifados().isEmpty()) atributos.remove("almoxarifados");
  else {var permitidos=atributos.putArray("almoxarifados");dados.almoxarifados().stream().map(UUID::toString).sorted().forEach(permitidos::add);}
  exigir(admin("PUT","/users/"+id,token,usuario),204);
  var remover=json.createArrayNode();for(var papel:diretos) if(PERFIS.contains(papel.path("name").asText())) remover.add(papel);
  if(!remover.isEmpty()) exigir(admin("DELETE","/users/"+id+"/role-mappings/realm",token,remover),204);
  var papel=admin("GET","/roles/"+dados.perfil().name(),token,null);exigir(papel,200);
  exigir(admin("POST","/users/"+id+"/role-mappings/realm",token,json.createArrayNode().add(ler(papel.body()))),204);
  exigir(admin("POST","/users/"+id+"/logout",token,null),204);return id;
 }
 public void definirAtivo(UUID org,UUID id,boolean ativo) {
  String token=token();var usuario=buscar(token,id);tenant(org,usuario);proteger(token,id);usuario.put("enabled",ativo);
  exigir(admin("PUT","/users/"+id,token,usuario),204);
  if(!ativo) exigir(admin("POST","/users/"+id+"/logout",token,null),204);
 }
}
