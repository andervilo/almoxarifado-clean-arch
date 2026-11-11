package com.almoxarifado.backend.estoque.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MovimentacaoEstoque {
    private final TipoMovimento tipo;
    private final BigDecimal quantidade;
    private final String codigoLote;
    private final LocalDateTime dataHora;

    public MovimentacaoEstoque(TipoMovimento tipo, BigDecimal quantidade, String codigoLote, LocalDateTime dataHora) {
        if (tipo == null) {
            throw new IllegalArgumentException("Tipo da movimentação é obrigatório");
        }
        if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantidade da movimentação deve ser maior que zero");
        }

        this.tipo = tipo;
        this.quantidade = quantidade;
        this.codigoLote = codigoLote;
        this.dataHora = dataHora != null ? dataHora : LocalDateTime.now();
    }

    public TipoMovimento getTipo() {
        return tipo;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public String getCodigoLote() {
        return codigoLote;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}
