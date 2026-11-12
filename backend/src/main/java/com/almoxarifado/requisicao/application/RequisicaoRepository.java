package com.almoxarifado.requisicao.application;
import java.util.*;
import com.almoxarifado.requisicao.domain.Requisicao;
public interface RequisicaoRepository {
 void criar(Requisicao requisicao);
 Optional<Requisicao> buscar(UUID org,UUID id,boolean bloquear);
 List<Requisicao> listar(UUID org,String solicitante,int limite,int deslocamento);
 void atualizar(Requisicao requisicao);
 void editar(Requisicao requisicao);
}
