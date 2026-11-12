package com.almoxarifado.organizacao.domain;

import java.util.Objects;

public class Usuario {
    private final String codigo;
    private final String nome;
    private final String email;
    private final PerfilUsuario perfil;
    private final String organizacaoId;
    private boolean ativo;

    public Usuario(String codigo, String nome, String email, PerfilUsuario perfil, String organizacaoId) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Código do usuário é obrigatório");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do usuário é obrigatório");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email do usuário é obrigatório");
        }
        if (perfil == null) {
            throw new IllegalArgumentException("Perfil do usuário é obrigatório");
        }
        if (organizacaoId == null || organizacaoId.isBlank()) {
            throw new IllegalArgumentException("Organização do usuário é obrigatória");
        }

        this.codigo = codigo;
        this.nome = nome;
        this.email = email;
        this.perfil = perfil;
        this.organizacaoId = organizacaoId;
        this.ativo = true;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public PerfilUsuario getPerfil() {
        return perfil;
    }

    public String getOrganizacaoId() {
        return organizacaoId;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void inativar() {
        this.ativo = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Usuario usuario = (Usuario) o;
        return Objects.equals(codigo, usuario.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }
}
