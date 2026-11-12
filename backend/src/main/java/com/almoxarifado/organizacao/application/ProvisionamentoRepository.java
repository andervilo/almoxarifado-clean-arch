package com.almoxarifado.organizacao.application;
import java.util.*;
import com.almoxarifado.organizacao.domain.*;
public interface ProvisionamentoRepository {
 void bloquear(UUID org,UUID id);
 Optional<ProvisionamentoIdentidade> buscar(UUID org,UUID id,boolean bloquear);
 void criar(ProvisionamentoIdentidade pedido);
 void estado(UUID org,UUID id,String status,UUID keycloakId,String erro);
 void bloquearUsuario(UUID org,UUID keycloakId);
 boolean possuiPosterior(UUID org,UUID keycloakId,UUID pedido);
 boolean atual(UUID org,UUID keycloakId,UUID pedido);
 void marcarAtual(UUID org,UUID keycloakId,UUID pedido);
}
