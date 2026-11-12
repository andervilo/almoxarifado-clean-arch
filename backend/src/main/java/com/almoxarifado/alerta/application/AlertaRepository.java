package com.almoxarifado.alerta.application;
import java.util.*;
import com.almoxarifado.alerta.domain.Alerta;
public interface AlertaRepository {
 List<Alerta> listar(UUID org,int limite,int deslocamento,Set<UUID> escopo);
 Optional<Alerta> buscar(UUID org,String chave,String revisao,Set<UUID> escopo);
 void dispensar(UUID org,String chave,String revisao,String usuario);
 Map<String,Object> dashboard(UUID org,Set<UUID> escopo);
}
