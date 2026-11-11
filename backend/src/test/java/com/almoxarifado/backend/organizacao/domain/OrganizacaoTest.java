package com.almoxarifado.backend.organizacao.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrganizacaoTest {

    @Test
    void devePermitirCriarOrganizacaoComSetores() {
        Organizacao organizacao = new Organizacao("Acme", "ACME");

        organizacao.adicionarSetor(new Setor("S-01", "Compras"));
        organizacao.adicionarSetor(new Setor("S-02", "Expedição"));

        assertEquals("Acme", organizacao.getNome());
        assertEquals(2, organizacao.getSetores().size());
        assertTrue(organizacao.isAtiva());
    }

    @Test
    void deveRejeitarNomeInvalido() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Organizacao("   ", "ACME")
        );

        assertEquals("Nome da organização é obrigatório", exception.getMessage());
    }

    @Test
    void deveRejeitarUsuarioSemPerfil() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new Usuario("U-1", "Maria", "maria@acme.com", null, "ORG-1")
        );

        assertEquals("Perfil do usuário é obrigatório", exception.getMessage());
    }

    @Test
    void deveInativarOrganizacaoQuandoSolicitado() {
        Organizacao organizacao = new Organizacao("Acme", "ACME");

        organizacao.inativar();

        assertFalse(organizacao.isAtiva());
    }
}
