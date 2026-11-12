package com.almoxarifado.shared.events;
import java.util.*;
import java.time.Instant;
import com.almoxarifado.shared.events.IntegrationEvent;
public record AjusteInventarioConcluido(UUID eventId,Instant occurredAt,UUID organizacaoId,int version,
 UUID inventarioId,UUID comandoId,boolean sucesso,String erro) implements IntegrationEvent {}
