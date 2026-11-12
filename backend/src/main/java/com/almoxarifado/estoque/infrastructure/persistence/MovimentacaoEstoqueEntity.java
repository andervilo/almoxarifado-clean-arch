package com.almoxarifado.estoque.infrastructure.persistence;

import com.almoxarifado.estoque.domain.TipoMovimento;
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
import org.hibernate.annotations.Immutable;

/**
 * Movimentação registrada é imutável (regra 7): não há setters e o Hibernate
 * nunca emite UPDATE para esta entidade. Correções são feitas via estorno ou ajuste.
 */
@Entity
@Immutable
@Table(schema = "estoque", name = "movimentacoes_estoque")
public class MovimentacaoEstoqueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TipoMovimento tipo;

    @Column(nullable = false, updatable = false)
    private BigDecimal quantidade;

    @Column(updatable = false)
    private String codigoLote;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, updatable = false)
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

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public String getCodigoLote() {
        return codigoLote;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public ItemEstoqueEntity getItem() {
        return item;
    }
}
