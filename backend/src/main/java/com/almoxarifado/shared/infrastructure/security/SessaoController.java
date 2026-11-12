package com.almoxarifado.shared.infrastructure.security;

import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class SessaoController {

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA', 'ADMIN_ORGANIZACAO', 'ALMOXARIFE', 'REQUISITANTE', 'GESTOR')")
    public SessaoResponse consultar(JwtAuthenticationToken autenticacao) {
        Object claim = autenticacao.getToken().getClaims().get("organizacao_id");
        UUID organizacaoId;
        try {
            if (!(claim instanceof String valor)) {
                throw new IllegalArgumentException();
            }
            organizacaoId = UUID.fromString(valor);
            if (!organizacaoId.toString().equalsIgnoreCase(valor)) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException("Token sem organização válida");
        }

        var perfis = autenticacao.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority.startsWith("ROLE_"))
            .map(authority -> authority.substring(5))
            .distinct()
            .sorted()
            .toList();
        return new SessaoResponse(autenticacao.getToken().getSubject(), autenticacao.getName(), organizacaoId, perfis);
    }
}
