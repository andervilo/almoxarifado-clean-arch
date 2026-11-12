package com.almoxarifado.estrutura.infrastructure.web;
import org.mapstruct.*;
import com.almoxarifado.estrutura.domain.*;
import com.almoxarifado.estrutura.infrastructure.web.EstruturaController.*;
@Mapper(componentModel="spring",unmappedTargetPolicy=ReportingPolicy.ERROR)
public interface EstruturaMapper {
 AlmoxarifadoResponse resposta(Almoxarifado valor);
 LocalResponse resposta(LocalArmazenamento valor);
}
