package com.almoxarifado.estoque.application;
import java.util.*;
import java.math.BigDecimal;
import com.almoxarifado.estoque.domain.PosicaoEstoque;
public interface EstoqueRepository {
 record ItemOperacional(String rastreabilidade,BigDecimal minimo) {}
 record Linha(UUID posicaoId,BigDecimal quantidade,BigDecimal custo) {}
 record Reserva(UUID posicaoId,BigDecimal quantidade) {}
 void bloquear(UUID org,UUID chave);
 ItemOperacional validarItem(UUID org,UUID item);
 UUID validarLocal(UUID org,UUID local);
 List<PosicaoEstoque> posicoes(UUID org,UUID item);
 void salvar(PosicaoEstoque posicao);
 Optional<String> assinatura(UUID org,UUID operacao);
 void operacao(UUID org,UUID operacao,ComandoEstoque comando,String usuario,List<Linha> linhas);
 List<Linha> linhas(UUID org,UUID operacao);
 List<Map<String,Object>> historico(UUID org,UUID item,int limite,int deslocamento,Set<UUID> escopo);
 List<Reserva> reservas(UUID org,UUID requisicao,UUID item);
 void reservar(UUID org,UUID requisicao,UUID posicao,BigDecimal quantidade);
 void concluirReserva(UUID org,UUID requisicao,UUID item,String status);
 void consumirReserva(UUID org,UUID requisicao,UUID posicao,java.math.BigDecimal restante);
 record Alocacao(UUID posicaoId,UUID localId,String lote,String serie,java.time.LocalDate validade,java.math.BigDecimal quantidade) {}
 List<Alocacao> alocacoes(UUID org,UUID requisicao,UUID item);
 long avancarRevisaoReserva(UUID org,UUID requisicao,UUID item);
 java.time.Instant registrarEntrega(UUID org,UUID operacao,UUID requisicao,UUID item,java.math.BigDecimal quantidade,java.math.BigDecimal custoTotal);
 record ConsumoVinculado(UUID requisicaoId,UUID itemId,java.math.BigDecimal quantidade,java.math.BigDecimal custoTotal) {}
 Optional<ConsumoVinculado> consumoDaOperacao(UUID org,UUID operacao);
 void bloquearLocal(UUID org,UUID local);
 record PosicaoLocal(UUID posicaoId,UUID itemId) {}
 List<PosicaoLocal> posicoesDoLocal(UUID org,UUID local);
 BigDecimal custo(UUID org,UUID item,UUID almoxarifado);
 BigDecimal ajustarCusto(UUID org,UUID item,UUID almoxarifado,BigDecimal delta,BigDecimal valorEntrada);
 void marcarEstorno(UUID org,UUID original,UUID estorno);
 boolean estornado(UUID org,UUID original);
 long revisao(UUID org,UUID posicao);
 boolean processado(UUID org,UUID evento);
 void concluirProcessamento(UUID org,UUID evento);
 List<Map<String,Object>> rastrear(UUID org,UUID item,String lote,String serie,int limite,int deslocamento,Set<UUID> escopo);
 List<Map<String,Object>> custos(UUID org,UUID item,Set<UUID> escopo);
 UUID almoxarifadoDoLocal(UUID org,UUID local);
}
