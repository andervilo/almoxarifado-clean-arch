package com.almoxarifado.catalogo.events;
import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;
import com.almoxarifado.shared.events.IntegrationEvent;
public record ItemAlterado(UUID eventId, Instant occurredAt, UUID organizacaoId, int version,
 UUID itemId, String codigo, String descricao, String rastreabilidade, BigDecimal estoqueMinimo,
 BigDecimal estoqueMaximo, boolean ativo) implements IntegrationEvent {}
