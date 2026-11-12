package com.almoxarifado.shared.infrastructure.security;

import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import com.almoxarifado.shared.application.ContextoUsuario;

@Component
public class ContextoUsuarioJwt implements ContextoUsuario {
    public java.util.Set<UUID> almoxarifadosPermitidos() {
        if(possuiPerfil("ADMIN_PLATAFORMA") || possuiPerfil("ADMIN_ORGANIZACAO")) return null;
        Object restrito=autenticacao().getToken().getClaims().get("escopo_restrito");
        if(restrito!=null && !(restrito instanceof Boolean)) throw new AccessDeniedException("Indicador de escopo invalido");
        Object valor=autenticacao().getToken().getClaims().get("almoxarifados");
        if(valor==null) return Boolean.TRUE.equals(restrito)?java.util.Set.of():null;
        if(!(valor instanceof java.util.Collection<?> lista) || lista.size()>1000)
            throw new AccessDeniedException("Escopo de almoxarifados invalido");
        var ids=new java.util.HashSet<UUID>();
        try {
            for(var item:lista) {
                if(!(item instanceof String texto)) throw new IllegalArgumentException();
                var id=UUID.fromString(texto);
                if(!id.toString().equalsIgnoreCase(texto)) throw new IllegalArgumentException();
                ids.add(id);
            }
        } catch(IllegalArgumentException ex) { throw new AccessDeniedException("Escopo de almoxarifados invalido"); }
        return java.util.Set.copyOf(ids);
    }
    private JwtAuthenticationToken autenticacao() {
        var autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (!(autenticacao instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()) {
            throw new AccessDeniedException("Autenticacao JWT obrigatoria");
        }
        return jwt;
    }
    public UUID organizacaoId() {
        Object valor = autenticacao().getToken().getClaims().get("organizacao_id");
        try {
            if (!(valor instanceof String texto)) throw new IllegalArgumentException();
            UUID id = UUID.fromString(texto);
            if (!id.toString().equalsIgnoreCase(texto)) throw new IllegalArgumentException();
            return id;
        } catch (IllegalArgumentException ex) {
            throw new AccessDeniedException("Organizacao invalida no token");
        }
    }
    public String usuarioId() {
        String id = autenticacao().getToken().getSubject();
        if (id == null || id.isBlank()) throw new AccessDeniedException("Identidade ausente no token");
        return id;
    }
    public boolean possuiPerfil(String perfil) {
        return autenticacao().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + perfil));
    }
}
