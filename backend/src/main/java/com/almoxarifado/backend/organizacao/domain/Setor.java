package com.almoxarifado.backend.organizacao.domain;

import java.util.Objects;

public class Setor {
    private final String codigo;
    private final String nome;

    public Setor(String codigo, String nome) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Código do setor é obrigatório");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do setor é obrigatório");
        }

        this.codigo = codigo;
        this.nome = nome;
    }

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
