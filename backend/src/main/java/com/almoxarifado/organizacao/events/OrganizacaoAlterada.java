package com.almoxarifado.organizacao.events;
import java.time.Instant;
import java.util.UUID;
import com.almoxarifado.shared.events.IntegrationEvent;
public record OrganizacaoAlterada(UUID eventId, Instant occurredAt, UUID organizacaoId,
    int version, String codigo, String nome, boolean ativa) implements IntegrationEvent {}
