package com.almoxarifado.requisicao.application;
import java.util.*;
import com.almoxarifado.requisicao.domain.Requisicao;
public interface RequisicaoRepository {
 void criar(Requisicao requisicao);
 Optional<Requisicao> buscar(UUID org,UUID id,boolean bloquear);
 List<Requisicao> listar(UUID org,String solicitante,int limite,int deslocamento,Set<UUID> escopo);
 void atualizar(Requisicao requisicao);
 void editar(Requisicao requisicao);
 UUID almoxarifadoDoLocal(UUID org,UUID local);
 void prepararEntrega(UUID org,UUID requisicao,UUID comando,List<Requisicao.Linha> linhas);
 void aplicarEntrega(UUID org,UUID requisicao,UUID comando);
 record ChaveEntrega(UUID requisicaoId,String assinatura,UUID comandoId) {}
 void bloquearChaveEntrega(UUID org,UUID chave);
 Optional<ChaveEntrega> chaveEntrega(UUID org,UUID chave);
 void registrarChaveEntrega(UUID org,UUID chave,UUID requisicao,String assinatura,UUID comando);
 record Alocacao(UUID itemId,UUID posicaoId,UUID localId,String lote,String serie,java.time.LocalDate validade,java.math.BigDecimal quantidade) {}
 List<Alocacao> alocacoes(UUID org,UUID requisicao);
 Optional<ChaveEntrega> chaveRealocacao(UUID org,UUID chave);
 void registrarChaveRealocacao(UUID org,UUID chave,UUID requisicao,String assinatura,UUID comando);
 record SetorReferencia(String codigo,String nome,String centroCusto) {}
 Optional<SetorReferencia> setor(UUID org,String codigo);
 List<Map<String,Object>> consumo(UUID org,java.time.LocalDate inicio,java.time.LocalDate fim,Set<UUID> escopo,int limite,int deslocamento);
}
