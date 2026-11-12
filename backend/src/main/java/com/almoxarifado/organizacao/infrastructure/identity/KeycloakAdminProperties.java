package com.almoxarifado.organizacao.infrastructure.identity;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
@ConfigurationProperties("almoxarifado.keycloak-admin")
public record KeycloakAdminProperties(@DefaultValue("false") boolean enabled,
 @DefaultValue("http://localhost:8181") URI baseUrl,@DefaultValue("almoxarifado") String realm,
 @DefaultValue("almoxarifado-admin") String clientId,@DefaultValue("") String clientSecret) {
 public KeycloakAdminProperties {
  if(baseUrl==null || baseUrl.getHost()==null || !java.util.Set.of("http","https").contains(baseUrl.getScheme()) || baseUrl.getUserInfo()!=null || baseUrl.getQuery()!=null || baseUrl.getFragment()!=null || !realm.matches("[a-zA-Z0-9_-]{1,120}")) throw new IllegalArgumentException("Configuracao administrativa do Keycloak invalida");
 }
 @Override public String toString() {return "KeycloakAdminProperties[enabled="+enabled+", realm="+realm+", clientId="+clientId+", clientSecret=REDACTED]";}
}
