package com.almoxarifado.estrutura.application;
import java.util.*;
import com.almoxarifado.shared.application.SnapshotEventos;
public interface SnapshotRepository {
 List<SnapshotEventos> carregar(UUID organizacaoId,String recurso,UUID apos,int tamanho);
}
