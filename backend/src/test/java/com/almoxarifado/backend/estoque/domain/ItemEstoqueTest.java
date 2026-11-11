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

    @Test
    void deveUsarLoteComValidadeMaisProximaNaSaidaPadrao() {
        ItemEstoque item = new ItemEstoque("MAT-005", "Adesivo", true, new BigDecimal("20.00"));
        item.adicionarLote(new LoteEstoque("LOTE-10", LocalDate.now().plusDays(30), new BigDecimal("10.00")));
        item.adicionarLote(new LoteEstoque("LOTE-11", LocalDate.now().plusDays(5), new BigDecimal("10.00")));

        item.registrarSaida(new BigDecimal("6.00"));

        assertEquals(new BigDecimal("4.00"), item.getLotes().get("LOTE-11").getQuantidade());
    }

    @Test
    void deveUsarFilaFifoQuandoItemNaoTemValidade() {
        ItemEstoque item = new ItemEstoque("MAT-006", "Parafuso", true, new BigDecimal("20.00"));
        item.adicionarLote(new LoteEstoque("LOTE-20", null, new BigDecimal("10.00")));
        item.adicionarLote(new LoteEstoque("LOTE-21", null, new BigDecimal("10.00")));

        item.registrarSaida(new BigDecimal("15.00"));

        assertEquals(new BigDecimal("0.00"), item.getLotes().get("LOTE-20").getQuantidade());
        assertEquals(new BigDecimal("5.00"), item.getLotes().get("LOTE-21").getQuantidade());
    }
}
