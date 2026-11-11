package com.almoxarifado.backend.estoque.infrastructure.persistence;

import com.almoxarifado.backend.estoque.domain.TipoMovimento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacoes_estoque")
public class MovimentacaoEstoqueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimento tipo;

    @Column(nullable = false)
    private BigDecimal quantidade;

    @Column
    private String codigoLote;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemEstoqueEntity item;

    protected MovimentacaoEstoqueEntity() {
    }

    public MovimentacaoEstoqueEntity(TipoMovimento tipo, BigDecimal quantidade, String codigoLote, LocalDateTime dataHora, ItemEstoqueEntity item) {
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.codigoLote = codigoLote;
        this.dataHora = dataHora;
        this.item = item;
    }

    public Long getId() {
        return id;
    }

    public TipoMovimento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimento tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public String getCodigoLote() {
        return codigoLote;
    }

    public void setCodigoLote(String codigoLote) {
        this.codigoLote = codigoLote;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public ItemEstoqueEntity getItem() {
        return item;
    }

    public void setItem(ItemEstoqueEntity item) {
        this.item = item;
    }
}
