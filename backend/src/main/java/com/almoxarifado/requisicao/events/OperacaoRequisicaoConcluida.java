package com.almoxarifado.requisicao.events;
import java.util.*;
import java.time.Instant;
import com.almoxarifado.shared.events.IntegrationEvent;
public record OperacaoRequisicaoConcluida(UUID eventId,Instant occurredAt,UUID organizacaoId,int version,
 UUID requisicaoId,UUID comandoId,boolean sucesso,String erro) implements IntegrationEvent {}
