package com.almoxarifado.organizacao.application;
import java.util.UUID;
import com.almoxarifado.organizacao.domain.DadosConta;
public interface ProvedorIdentidade {
 boolean habilitado();
 UUID preparar(UUID organizacao,UUID provisionamento,DadosConta dados);
 void definirAtivo(UUID organizacao,UUID usuario,boolean ativo);
}
