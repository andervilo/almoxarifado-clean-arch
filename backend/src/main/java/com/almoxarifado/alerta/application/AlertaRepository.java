package com.almoxarifado.alerta.application;
import java.util.*;
import com.almoxarifado.alerta.domain.Alerta;
public interface AlertaRepository {
 List<Alerta> listar(UUID org,int limite,int deslocamento);
 Optional<Alerta> buscar(UUID org,String chave,String revisao);
 void dispensar(UUID org,String chave,String revisao,String usuario);
 Map<String,Object> dashboard(UUID org);
}
