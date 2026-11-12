package com.almoxarifado.shared.infrastructure.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SessaoController.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(CorsProperties.class)
class SessaoControllerTest {

    private static final String ORGANIZACAO = "0190f5a0-0000-7000-8000-000000000001";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder decoder;

    @Test
    void exigeBearerToken() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void rejeitaTokenInvalido() throws Exception {
        when(decoder.decode("invalido")).thenThrow(new BadJwtException("Assinatura inválida"));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer invalido"))
            .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN_PLATAFORMA", "ADMIN_ORGANIZACAO", "ALMOXARIFE", "REQUISITANTE", "GESTOR"})
    void expoeSessaoDoTokenParaPerfisPermitidos(String perfil) throws Exception {
        when(decoder.decode("valido")).thenReturn(jwt(perfil, ORGANIZACAO));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer valido"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.usuarioId").value("usuario-keycloak"))
            .andExpect(jsonPath("$.nomeUsuario").value("operador"))
            .andExpect(jsonPath("$.organizacaoId").value(ORGANIZACAO))
            .andExpect(jsonPath("$.perfis[0]").value(perfil));
    }

    @Test
    void rejeitaPerfilSemPermissao() throws Exception {
        when(decoder.decode("valido")).thenReturn(jwt("OUTRO_PERFIL", ORGANIZACAO));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer valido"))
            .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalida", "1-1-1-1-1"})
    void rejeitaOrganizacaoInvalida(String organizacao) throws Exception {
        when(decoder.decode("valido")).thenReturn(jwt("ALMOXARIFE", organizacao));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer valido"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
    }

    @Test
    void rejeitaOrganizacaoAusente() throws Exception {
        when(decoder.decode("valido")).thenReturn(jwt("ALMOXARIFE", null));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer valido"))
            .andExpect(status().isForbidden());
    }

    @Test
    void permitePreflightDaInterface() throws Exception {
        mvc.perform(options("/api/v1/auth/me")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    void rejeitaOrigemNaoAutorizada() throws Exception {
        mvc.perform(options("/api/v1/auth/me")
                .header("Origin", "https://outra-origem.example")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden());
    }

    private Jwt jwt(String perfil, String organizacao) {
        var builder = Jwt.withTokenValue("valido").header("alg", "RS256")
            .subject("usuario-keycloak")
            .claim("preferred_username", "operador")
            .claim("realm_access", Map.of("roles", List.of(perfil)))
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300));
        if (organizacao != null) {
            builder.claim("organizacao_id", organizacao);
        }
        return builder.build();
    }
}
