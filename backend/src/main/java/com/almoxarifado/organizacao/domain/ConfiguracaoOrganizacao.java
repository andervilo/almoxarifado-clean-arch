package com.almoxarifado.organizacao.domain;
public record ConfiguracaoOrganizacao(int diasAlertaValidade) {
    public ConfiguracaoOrganizacao {
        if(diasAlertaValidade<1 || diasAlertaValidade>365) throw new IllegalArgumentException("Prazo de alerta deve estar entre 1 e 365 dias");
    }
}
