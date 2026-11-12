package com.almoxarifado.shared.events;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import com.almoxarifado.shared.events.IntegrationEvent;
public record AjusteInventarioSolicitado(UUID eventId,Instant occurredAt,UUID organizacaoId,int version,
 UUID inventarioId,UUID localId,String usuarioId,String justificativa,List<Linha> linhas) implements IntegrationEvent {
 public AjusteInventarioSolicitado { linhas=List.copyOf(linhas); }
 public record Linha(UUID posicaoId,UUID itemId,long revisao,BigDecimal sistema,BigDecimal contada,
  String lote,String serie,LocalDate validade,BigDecimal custo) {}
}
