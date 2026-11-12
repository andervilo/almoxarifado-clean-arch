package com.almoxarifado.inventario.application;
import java.util.*;
import com.almoxarifado.inventario.domain.Contagem;
public interface InventarioRepository {
 record Inventario(UUID id,UUID localId,String status,UUID comandoId,String erro,List<Contagem> contagens,boolean contagemCega) {}
 List<Contagem> snapshot(UUID org,UUID local);
 void criar(UUID org,UUID id,UUID local,List<Contagem> contagens,boolean contagemCega);
 Optional<Inventario> buscar(UUID org,UUID id,boolean bloquear);
 void contar(UUID org,UUID inventario,Contagem contagem);
 void estado(UUID org,UUID id,String status,UUID comando,String erro);
 List<UUID> listar(UUID org,int limite,int deslocamento,Set<UUID> escopo);
 UUID almoxarifadoDoLocal(UUID org,UUID local);
 void adicionarContagem(UUID org,UUID inventario,Contagem contagem);
}
