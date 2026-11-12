package com.almoxarifado.estrutura.events;
import java.util.UUID;
import java.time.Instant;
import com.almoxarifado.shared.events.IntegrationEvent;
public record LocalAlterado(UUID eventId, Instant occurredAt, UUID organizacaoId, int version,
 UUID localId, UUID almoxarifadoId, boolean ativo) implements IntegrationEvent {}
