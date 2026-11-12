package com.almoxarifado.requisicao.events;
import java.util.*;
import java.time.Instant;
import java.math.BigDecimal;
import com.almoxarifado.shared.events.IntegrationEvent;
public record RealocacaoReservaSolicitada(UUID eventId,Instant occurredAt,UUID organizacaoId,int version,
 UUID requisicaoId,UUID itemId,UUID localId,String usuarioId,String motivo,List<Alocacao> alocacoes) implements IntegrationEvent {
 public RealocacaoReservaSolicitada {alocacoes=List.copyOf(alocacoes);}
 public record Alocacao(UUID posicaoId,BigDecimal quantidade) {}
}
