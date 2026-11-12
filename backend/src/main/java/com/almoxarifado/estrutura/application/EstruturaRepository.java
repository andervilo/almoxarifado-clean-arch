package com.almoxarifado.estrutura.application;
import java.util.*;
import com.almoxarifado.estrutura.domain.*;
public interface EstruturaRepository {
 boolean organizacaoAtiva(UUID org);
 Optional<Almoxarifado> almoxarifado(UUID org, UUID id);
 Optional<LocalArmazenamento> local(UUID org, UUID id);
 List<Almoxarifado> almoxarifados(UUID org,int limite,int deslocamento,Set<UUID> escopo);
 List<LocalArmazenamento> locais(UUID org,UUID almoxarifado,int limite,int deslocamento);
 void salvar(Almoxarifado valor);
 void salvar(LocalArmazenamento valor);
}
