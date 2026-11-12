package com.almoxarifado.requisicao.events;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import com.almoxarifado.shared.events.IntegrationEvent;
public record AlocacoesRequisicaoAtualizadas(UUID eventId,Instant occurredAt,UUID organizacaoId,int version,
 UUID requisicaoId,UUID itemId,long revisao,List<Alocacao> alocacoes) implements IntegrationEvent {
 public AlocacoesRequisicaoAtualizadas {alocacoes=List.copyOf(alocacoes);}
 public record Alocacao(UUID posicaoId,UUID localId,String lote,String serie,LocalDate validade,BigDecimal quantidade) {}
}
