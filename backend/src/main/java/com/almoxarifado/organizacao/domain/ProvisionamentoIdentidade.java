package com.almoxarifado.organizacao.domain;
import java.util.UUID;
public record ProvisionamentoIdentidade(UUID id,UUID organizacaoId,String ator,DadosConta dados,String status,UUID keycloakId,String erro) {}
