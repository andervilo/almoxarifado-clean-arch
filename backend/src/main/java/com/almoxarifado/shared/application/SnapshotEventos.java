package com.almoxarifado.shared.application;
import java.util.*;
import com.almoxarifado.shared.events.IntegrationEvent;
public record SnapshotEventos(UUID id,List<IntegrationEvent> eventos) {
 public SnapshotEventos { eventos=List.copyOf(eventos); }
}
