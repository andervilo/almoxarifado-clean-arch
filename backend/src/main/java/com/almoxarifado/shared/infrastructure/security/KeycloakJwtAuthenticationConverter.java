package com.almoxarifado.shared.infrastructure.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Converte o JWT emitido pelo Keycloak em authorities do Spring Security.
 * Roles do realm ({@code realm_access.roles}) viram {@code ROLE_<ROLE>},
 * permitindo {@code @PreAuthorize("hasRole('ALMOXARIFE')")}.
 */
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String CLAIM_REALM_ACCESS = "realm_access";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_USERNAME = "preferred_username";

    private final JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
            scopesConverter.convert(jwt).stream(),
            extrairRolesDoRealm(jwt).stream()
        ).toList();

        String nome = jwt.getClaimAsString(CLAIM_USERNAME);
        if (nome == null || nome.isBlank()) {
            nome = jwt.getSubject();
        }
        return new JwtAuthenticationToken(jwt, authorities, nome);
    }

    private List<GrantedAuthority> extrairRolesDoRealm(Jwt jwt) {
        if (!(jwt.getClaims().get(CLAIM_REALM_ACCESS) instanceof Map<?, ?> realmAccess)
            || !(realmAccess.get(CLAIM_ROLES) instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .filter(role -> !role.isBlank())
            .distinct()
            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
            .toList();
    }
}
