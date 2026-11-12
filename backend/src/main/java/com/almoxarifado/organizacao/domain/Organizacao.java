package com.almoxarifado.organizacao.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Organizacao {
    private final String codigo;
    private final String nome;
    private final Map<String, Setor> setores = new LinkedHashMap<>();
    private boolean ativa;

    public Organizacao(String nome, String codigo) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome da organização é obrigatório");
        }
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Código da organização é obrigatório");
        }

        this.nome = nome;
        this.codigo = codigo;
        this.ativa = true;
    }

    public void adicionarSetor(Setor setor) {
        if (setor == null) {
            throw new IllegalArgumentException("Setor inválido");
        }
        if (setores.containsKey(setor.getCodigo())) {
            throw new IllegalStateException("Setor já cadastrado para a organização");
        }

        setores.put(setor.getCodigo(), setor);
    }

    public void removerSetor(String codigoSetor) {
        if (codigoSetor == null || codigoSetor.isBlank()) {
            throw new IllegalArgumentException("Código do setor é obrigatório");
        }

        setores.remove(codigoSetor);
    }

    public void inativar() {
        this.ativa = false;
    }

    public void ativar() {
        this.ativa = true;
    }

    public String getNome() {
        return nome;
    }

    public String getCodigo() {
        return codigo;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public List<Setor> getSetores() {
        return Collections.unmodifiableList(new ArrayList<>(setores.values()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Organizacao that = (Organizacao) o;
        return Objects.equals(codigo, that.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }
}
