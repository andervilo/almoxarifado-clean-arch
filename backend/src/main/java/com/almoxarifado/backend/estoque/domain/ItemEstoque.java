package com.almoxarifado.backend.estoque.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ItemEstoque {
    private final String codigo;
    private final String descricao;
    private final boolean ativo;
    private BigDecimal saldo;
    private final Map<String, LoteEstoque> lotes = new LinkedHashMap<>();
    private final List<MovimentacaoEstoque> historicoMovimentacoes = new ArrayList<>();

    public ItemEstoque(String codigo, String descricao, boolean ativo, BigDecimal saldo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Código do item é obrigatório");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException("Descrição do item é obrigatória");
        }
        if (saldo == null || saldo.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Saldo inicial não pode ser negativo");
        }

        this.codigo = codigo;
        this.descricao = descricao;
        this.ativo = ativo;
        this.saldo = saldo;
    }

    public void registrarEntrada(BigDecimal quantidade) {
        validarQuantidade(quantidade, "entrada");
        if (!ativo) {
            throw new IllegalStateException("Item inativo não pode receber movimentação");
        }
        saldo = saldo.add(quantidade);
        historicoMovimentacoes.add(new MovimentacaoEstoque(TipoMovimento.ENTRADA, quantidade, null, LocalDateTime.now()));
    }

    public void registrarSaida(BigDecimal quantidade) {
        registrarSaida(quantidade, null);
    }

    public void registrarSaida(BigDecimal quantidade, String codigoLote) {
        validarQuantidade(quantidade, "saída");
        if (!ativo) {
            throw new IllegalStateException("Item inativo não pode ser movimentado");
        }

        if (saldo.subtract(quantidade).compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("saldo insuficiente para a saída solicitada");
        }

        if (codigoLote != null) {
            LoteEstoque lote = lotes.get(codigoLote);
            if (lote == null) {
                throw new IllegalStateException("Lote informado não existe para este item");
            }
            if (lote.isVencido()) {
                throw new IllegalStateException("Lote vencido não pode ser utilizado em saída");
            }
        }

        saldo = saldo.subtract(quantidade);
        historicoMovimentacoes.add(new MovimentacaoEstoque(TipoMovimento.SAIDA, quantidade, codigoLote, LocalDateTime.now()));
    }

    public void adicionarLote(LoteEstoque lote) {
        if (lote == null) {
            throw new IllegalArgumentException("Lote inválido");
        }
        lotes.put(lote.getCodigo(), lote);
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public List<MovimentacaoEstoque> getHistoricoMovimentacoes() {
        return Collections.unmodifiableList(historicoMovimentacoes);
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    private void validarQuantidade(BigDecimal quantidade, String tipo) {
        if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantidade de " + tipo + " deve ser maior que zero");
        }
    }
}
