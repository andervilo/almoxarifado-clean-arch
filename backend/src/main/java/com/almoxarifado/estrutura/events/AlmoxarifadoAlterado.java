package com.almoxarifado.estrutura.events;
import java.util.UUID;
import java.time.Instant;
import com.almoxarifado.shared.events.IntegrationEvent;
public record AlmoxarifadoAlterado(UUID eventId, Instant occurredAt, UUID organizacaoId, int version,
 UUID almoxarifadoId, boolean ativo) implements IntegrationEvent {}
