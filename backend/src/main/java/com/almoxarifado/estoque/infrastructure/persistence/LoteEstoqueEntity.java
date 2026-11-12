package com.almoxarifado.estoque.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    schema = "estoque",
    name = "lotes_estoque",
    uniqueConstraints = @UniqueConstraint(columnNames = {"item_id", "codigo"})
)
public class LoteEstoqueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String codigo;

    @Column
    private LocalDate validade;

    @Column(nullable = false)
    private BigDecimal quantidade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemEstoqueEntity item;

    protected LoteEstoqueEntity() {
    }

    public LoteEstoqueEntity(String codigo, LocalDate validade, BigDecimal quantidade, ItemEstoqueEntity item) {
        this.codigo = codigo;
        this.validade = validade;
        this.quantidade = quantidade;
        this.item = item;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public LocalDate getValidade() {
        return validade;
    }

    public void setValidade(LocalDate validade) {
        this.validade = validade;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }

    public ItemEstoqueEntity getItem() {
        return item;
    }

    public void setItem(ItemEstoqueEntity item) {
        this.item = item;
    }
}
