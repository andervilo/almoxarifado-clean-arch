package com.almoxarifado.organizacao.domain;

import java.util.Objects;

public class Setor {
    private final String codigo;
    private final String nome;
    private final String centroCusto;

    public Setor(String codigo, String nome) { this(codigo,nome,""); }

    public Setor(String codigo, String nome, String centroCusto) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Código do setor é obrigatório");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do setor é obrigatório");
        }

        this.codigo = codigo;
        this.nome = nome;
        this.centroCusto = centroCusto==null?"":centroCusto;
        if(this.centroCusto.length()>120) throw new IllegalArgumentException("Centro de custo muito longo");
    }

    public String getCentroCusto() { return centroCusto; }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Setor setor = (Setor) o;
        return Objects.equals(codigo, setor.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }
}
