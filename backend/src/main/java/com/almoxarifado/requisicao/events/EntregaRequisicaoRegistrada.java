package com.almoxarifado.requisicao.events;
import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;
import com.almoxarifado.shared.events.IntegrationEvent;
public record EntregaRequisicaoRegistrada(UUID eventId,Instant occurredAt,UUID organizacaoId,int version,
 UUID requisicaoId,UUID itemId,UUID operacaoId,BigDecimal quantidade,BigDecimal custoTotal,Instant efetivadoEm) implements IntegrationEvent {}
