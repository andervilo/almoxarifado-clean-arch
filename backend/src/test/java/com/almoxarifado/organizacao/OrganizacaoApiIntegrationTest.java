package com.almoxarifado.organizacao;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import com.almoxarifado.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class OrganizacaoApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private RequestPostProcessor usuario(UUID org,String perfil) {
        return jwt().jwt(j->j.subject("operador").claim("organizacao_id",org.toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_"+perfil));
    }
    private void criar(UUID org) throws Exception {
        mvc.perform(post("/api/v1/organizacoes").with(usuario(org,"ADMIN_PLATAFORMA"))
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"id":"%s","nome":"Empresa","codigo":"%s"}
                """.formatted(org,org))).andExpect(status().isCreated());
    }
    @Test
    void isolaSetoresEUsuariosPorOrganizacaoEAudita() throws Exception {
        UUID a=UUID.randomUUID(), b=UUID.randomUUID(); criar(a); criar(b);
        for (UUID org : new UUID[]{a,b}) {
            mvc.perform(put("/api/v1/setores").with(usuario(org,"ADMIN_ORGANIZACAO"))
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"codigo":"COMPRAS","nome":"%s"}
                    """.formatted(org))).andExpect(status().isOk());
            mvc.perform(put("/api/v1/usuarios").with(usuario(org,"ADMIN_ORGANIZACAO"))
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"codigo":"mesma-identidade","nome":"%s","email":"teste@empresa.local","perfil":"ALMOXARIFE","ativo":true}
                    """.formatted(org))).andExpect(status().isOk());
            mvc.perform(get("/api/v1/setores").with(usuario(org,"GESTOR")))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].nome").value(org.toString()));
            mvc.perform(get("/api/v1/usuarios").with(usuario(org,"ADMIN_ORGANIZACAO")))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].nome").value(org.toString()));
            assertThat(jdbc.queryForObject("SELECT count(*) FROM shared.auditoria WHERE organizacao_id=?",Integer.class,org)).isEqualTo(3);
        }
        assertThatThrownBy(()->jdbc.update("DELETE FROM shared.auditoria WHERE organizacao_id=?",a))
            .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
    @Test
    void impedeElevacaoDePerfilEEscritaSemPermissao() throws Exception {
        UUID org=UUID.randomUUID(); criar(org);
        mvc.perform(post("/api/v1/organizacoes").with(usuario(org,"ADMIN_ORGANIZACAO"))
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"id":"%s","nome":"Empresa","codigo":"outra"}
                """.formatted(UUID.randomUUID()))).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/setores").with(usuario(org,"REQUISITANTE"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"S\",\"nome\":\"S\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/usuarios").with(usuario(org,"ADMIN_ORGANIZACAO"))
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"codigo":"u","nome":"Usuario","email":"u@empresa.local","perfil":"ADMIN_PLATAFORMA","ativo":true}
                """)).andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM shared.auditoria WHERE organizacao_id=?",Integer.class,org)).isEqualTo(1);
    }
    @Test
    void exigeTenantValidoEBloqueiaOrganizacaoInativa() throws Exception {
        mvc.perform(get("/api/v1/organizacao").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_GESTOR")))).andExpect(status().isForbidden());
        UUID org=UUID.randomUUID(); criar(org);
        mvc.perform(put("/api/v1/organizacao").with(usuario(org,"ADMIN_ORGANIZACAO"))
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"nome":"Empresa","codigo":"%s","ativa":false}
                """.formatted(org))).andExpect(status().isOk());
        mvc.perform(put("/api/v1/setores").with(usuario(org,"ADMIN_ORGANIZACAO"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"S\",\"nome\":\"S\"}"))
            .andExpect(status().isUnprocessableEntity());
    }
}
