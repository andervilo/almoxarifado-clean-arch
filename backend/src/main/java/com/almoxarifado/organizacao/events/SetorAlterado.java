package com.almoxarifado.organizacao.events;
import java.util.UUID;
import java.time.Instant;
import com.almoxarifado.shared.events.IntegrationEvent;
public record SetorAlterado(UUID eventId,Instant occurredAt,UUID organizacaoId,int version,String codigo,String nome,String centroCusto) implements IntegrationEvent {}
