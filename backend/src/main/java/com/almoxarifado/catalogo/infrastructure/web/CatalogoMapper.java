package com.almoxarifado.catalogo.infrastructure.web;
import org.mapstruct.*;
import com.almoxarifado.catalogo.domain.*;
import com.almoxarifado.catalogo.infrastructure.web.CatalogoController.*;
@Mapper(componentModel="spring",unmappedTargetPolicy=ReportingPolicy.ERROR)
public interface CatalogoMapper {
 CadastroResponse resposta(Categoria valor);
 CadastroResponse resposta(UnidadeMedida valor);
 CadastroResponse resposta(Fornecedor valor);
 ItemResponse resposta(Item valor);
}
