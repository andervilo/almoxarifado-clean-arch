package com.almoxarifado.estoque.infrastructure.messaging;
import org.springframework.stereotype.Component;
import org.springframework.modulith.events.ApplicationModuleListener;
import com.almoxarifado.shared.events.*;
import com.almoxarifado.estoque.application.ProcessarInventario;
import com.almoxarifado.shared.application.PublicadorEventos;
import com.almoxarifado.shared.domain.*;
@Component
public class InventarioListener {
 private final ProcessarInventario processar;private final PublicadorEventos eventos;
 public InventarioListener(ProcessarInventario processar,PublicadorEventos eventos) { this.processar=processar;this.eventos=eventos; }
 @ApplicationModuleListener public void receber(AjusteInventarioSolicitado e) {
  String erro=null;
  try { processar.executar(e); } catch(RegraNegocioException ex) { erro=ex.getCodigo(); } catch(RecursoNaoEncontradoException ex) { erro="REFERENCIA_INDISPONIVEL"; }
  eventos.publicar(new AjusteInventarioConcluido(e.eventId(),java.time.Instant.now(),e.organizacaoId(),1,e.inventarioId(),e.eventId(),erro==null,erro));
 }
}
