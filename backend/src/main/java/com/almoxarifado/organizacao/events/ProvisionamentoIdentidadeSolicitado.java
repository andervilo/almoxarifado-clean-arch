package com.almoxarifado.organizacao.events;
import java.util.UUID;
import java.time.Instant;
import com.almoxarifado.shared.events.IntegrationEvent;
public record ProvisionamentoIdentidadeSolicitado(UUID eventId,Instant occurredAt,UUID organizacaoId,int version,UUID provisionamentoId) implements IntegrationEvent {}
