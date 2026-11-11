package com.almoxarifado.backend.estoque.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ItemEstoqueTest {

    @Test
    void devePermitirEntradaQuandoItemEstaAtivo() {
        ItemEstoque item = new ItemEstoque("MAT-001", "Parafuso", true, new BigDecimal("10.00"));

        item.registrarEntrada(new BigDecimal("5.00"));

        assertEquals(new BigDecimal("15.00"), item.getSaldo());
    }

    @Test
    void deveNegarSaidaQueDeixariaSaldoNegativo() {
        ItemEstoque item = new ItemEstoque("MAT-002", "Porca", true, new BigDecimal("10.00"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> item.registrarSaida(new BigDecimal("11.00")));

        assertTrue(exception.getMessage().contains("saldo"));
    }

    @Test
    void deveBloquearSaidaDeLoteVencido() {
        ItemEstoque item = new ItemEstoque("MAT-003", "Tinta", true, new BigDecimal("8.00"));
        item.adicionarLote(new LoteEstoque("LOTE-01", LocalDate.now().minusDays(1), new BigDecimal("8.00")));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> item.registrarSaida(new BigDecimal("1.00"), "LOTE-01"));

        assertTrue(exception.getMessage().contains("vencido"));
    }

    @Test
    void deveRegistrarHistoricoDeMovimentacoes() {
        ItemEstoque item = new ItemEstoque("MAT-004", "Fita", true, new BigDecimal("10.00"));

        item.registrarEntrada(new BigDecimal("5.00"));
        item.registrarSaida(new BigDecimal("2.00"));

        assertEquals(2, item.getHistoricoMovimentacoes().size());
        assertEquals(TipoMovimento.ENTRADA, item.getHistoricoMovimentacoes().getFirst().getTipo());
        assertEquals(TipoMovimento.SAIDA, item.getHistoricoMovimentacoes().get(1).getTipo());
    }
}
