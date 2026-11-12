package com.almoxarifado.requisicao.events;
import java.util.*;
import java.math.BigDecimal;
import java.time.Instant;
import com.almoxarifado.shared.events.IntegrationEvent;
public record OperacaoRequisicaoSolicitada(UUID eventId,Instant occurredAt,UUID organizacaoId,int version,
 UUID requisicaoId,String usuarioId,String operacao,UUID localId,List<Linha> linhas) implements IntegrationEvent {
 public OperacaoRequisicaoSolicitada { linhas=List.copyOf(linhas); }
 public record Linha(UUID itemId,BigDecimal quantidade) {}
}
