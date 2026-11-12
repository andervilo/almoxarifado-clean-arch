package com.almoxarifado.shared.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakJwtAuthenticationConverterTest {

    private final KeycloakJwtAuthenticationConverter converter = new KeycloakJwtAuthenticationConverter();

    @Test
    void deveConverterRolesDoRealmEmAuthoritiesComPrefixoRole() {
        Jwt jwt = jwt(Map.of(
            "sub", "123",
            "preferred_username", "almoxarife",
            "realm_access", Map.of("roles", List.of("ALMOXARIFE", "GESTOR"))
        ));

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getName()).isEqualTo("almoxarife");
        assertThat(token.getAuthorities()).extracting(GrantedAuthority::getAuthority)
            .contains("ROLE_ALMOXARIFE", "ROLE_GESTOR");
    }

    @Test
    void deveRetornarSemRolesQuandoClaimRealmAccessAusente() {
        Jwt jwt = jwt(Map.of("sub", "123"));

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getName()).isEqualTo("123");
        assertThat(token.getAuthorities()).extracting(GrantedAuthority::getAuthority)
            .noneMatch(a -> a.startsWith("ROLE_"));
    }

    @Test
    void devePreservarScopesEIgnorarRolesMalformadasOuDuplicadas() {
        var token = converter.convert(jwt(Map.of(
            "sub", "123", "scope", "openid profile", "preferred_username", "",
            "realm_access", Map.of("roles", List.of("ALMOXARIFE", "ALMOXARIFE", "", 42))
        )));
        assertThat(token.getName()).isEqualTo("123");
        assertThat(token.getAuthorities()).extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("SCOPE_openid", "SCOPE_profile", "ROLE_ALMOXARIFE");
    }

    @Test
    void deveIgnorarRealmAccessQueNaoSejaObjeto() {
        var token = converter.convert(jwt(Map.of("sub", "123", "realm_access", "invalido")));
        assertThat(token.getAuthorities()).isEmpty();
    }

    private Jwt jwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .claims(c -> c.putAll(claims))
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
    }
}
