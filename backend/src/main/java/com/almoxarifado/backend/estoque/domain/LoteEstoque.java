package com.almoxarifado.backend.estoque.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class LoteEstoque {
    private final String codigo;
    private final LocalDate validade;
    private BigDecimal quantidade;

    public LoteEstoque(String codigo, LocalDate validade, BigDecimal quantidade) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Código do lote é obrigatório");
        }
        if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantidade do lote deve ser maior que zero");
        }

        this.codigo = codigo;
        this.validade = validade;
        this.quantidade = quantidade;
    }

    public String getCodigo() {
        return codigo;
    }

    public LocalDate getValidade() {
        return validade;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void debitar(BigDecimal quantidadeSolicitada) {
        if (quantidadeSolicitada == null || quantidadeSolicitada.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantidade da saída do lote deve ser maior que zero");
        }
        if (quantidade.compareTo(quantidadeSolicitada) < 0) {
            throw new IllegalStateException("Quantidade insuficiente no lote para a saída solicitada");
        }
        quantidade = quantidade.subtract(quantidadeSolicitada);
    }

    public boolean isVencido() {
        return validade != null && LocalDate.now().isAfter(validade);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoteEstoque that = (LoteEstoque) o;
        return Objects.equals(codigo, that.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }
}
