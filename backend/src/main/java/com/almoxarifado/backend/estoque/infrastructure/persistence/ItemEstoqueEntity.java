package com.almoxarifado.backend.estoque.infrastructure.persistence;

import com.almoxarifado.backend.estoque.domain.TipoMovimento;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "itens_estoque")
public class ItemEstoqueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private boolean ativo;

    @Column(nullable = false)
    private BigDecimal saldo;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LoteEstoqueEntity> lotes = new ArrayList<>();

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MovimentacaoEstoqueEntity> movimentacoes = new ArrayList<>();

    protected ItemEstoqueEntity() {
    }

    public ItemEstoqueEntity(String codigo, String descricao, boolean ativo, BigDecimal saldo) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.ativo = ativo;
        this.saldo = saldo;
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

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public List<LoteEstoqueEntity> getLotes() {
        return lotes;
    }

    public void setLotes(List<LoteEstoqueEntity> lotes) {
        this.lotes = lotes;
    }

    public List<MovimentacaoEstoqueEntity> getMovimentacoes() {
        return movimentacoes;
    }

    public void setMovimentacoes(List<MovimentacaoEstoqueEntity> movimentacoes) {
        this.movimentacoes = movimentacoes;
    }
}
