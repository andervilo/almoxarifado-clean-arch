package com.almoxarifado.shared.events;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import com.almoxarifado.shared.events.IntegrationEvent;
public record AjusteInventarioSolicitado(UUID eventId,Instant occurredAt,UUID organizacaoId,int version,
 UUID inventarioId,UUID localId,String usuarioId,String justificativa,List<Linha> linhas,boolean localCompleto) implements IntegrationEvent {
 public AjusteInventarioSolicitado(UUID eventId,Instant occurredAt,UUID org,int version,UUID inventario,UUID local,String usuario,String motivo,List<Linha> linhas) {
  this(eventId,occurredAt,org,version,inventario,local,usuario,motivo,linhas,false);
 }
 public AjusteInventarioSolicitado { linhas=List.copyOf(linhas); }
 public record Linha(UUID posicaoId,UUID itemId,long revisao,BigDecimal sistema,BigDecimal contada,
  String lote,String serie,LocalDate validade,BigDecimal custo) {}
}
