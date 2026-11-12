package com.almoxarifado.catalogo.application;
import java.util.*;
import com.almoxarifado.catalogo.domain.*;
public interface CatalogoRepository {
 boolean organizacaoAtiva(UUID org);
 Optional<Categoria> buscarCategoria(UUID org,UUID id);
 List<Categoria> listarCategoria(UUID org,int limite,int deslocamento);
 void salvar(Categoria valor);
 Optional<UnidadeMedida> buscarUnidadeMedida(UUID org,UUID id);
 List<UnidadeMedida> listarUnidadeMedida(UUID org,int limite,int deslocamento);
 void salvar(UnidadeMedida valor);
 Optional<Fornecedor> buscarFornecedor(UUID org,UUID id);
 List<Fornecedor> listarFornecedor(UUID org,int limite,int deslocamento);
 void salvar(Fornecedor valor);
 Optional<Item> buscarItem(UUID org,UUID id);
 List<Item> listarItem(UUID org,int limite,int deslocamento);
 void salvar(Item valor);
}
