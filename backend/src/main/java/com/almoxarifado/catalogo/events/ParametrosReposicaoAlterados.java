package com.almoxarifado.catalogo.events;
import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;
import com.almoxarifado.shared.events.IntegrationEvent;
public record ParametrosReposicaoAlterados(UUID eventId,Instant occurredAt,UUID organizacaoId,int version,
 UUID itemId,UUID almoxarifadoId,BigDecimal minimo,BigDecimal maximo) implements IntegrationEvent {}
