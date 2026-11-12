package com.almoxarifado.estoque.events;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.*;
import com.almoxarifado.shared.events.IntegrationEvent;
public record SaldoAlterado(UUID eventId,Instant occurredAt,UUID organizacaoId,int version,UUID posicaoId,
 UUID itemId,UUID localId,BigDecimal quantidade,BigDecimal reservado,String lote,LocalDate validade,
 String serie,BigDecimal custo,long revisao) implements IntegrationEvent {}
