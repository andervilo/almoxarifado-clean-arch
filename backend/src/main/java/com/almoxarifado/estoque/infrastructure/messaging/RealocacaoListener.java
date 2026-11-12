package com.almoxarifado.estoque.infrastructure.messaging;
import org.springframework.stereotype.Component;
import org.springframework.modulith.events.ApplicationModuleListener;
import com.almoxarifado.estoque.application.ProcessarRealocacao;
import com.almoxarifado.requisicao.events.*;
import com.almoxarifado.shared.application.PublicadorEventos;
import com.almoxarifado.shared.domain.*;
@Component
public class RealocacaoListener {
 private final ProcessarRealocacao processar;private final PublicadorEventos eventos;
 public RealocacaoListener(ProcessarRealocacao processar,PublicadorEventos eventos) {this.processar=processar;this.eventos=eventos;}
 @ApplicationModuleListener public void receber(RealocacaoReservaSolicitada e) {
  String erro=null;
  try {processar.executar(e);}catch(RegraNegocioException ex){erro=ex.getCodigo();}catch(RecursoNaoEncontradoException ex){erro="REFERENCIA_INDISPONIVEL";}
  eventos.publicar(new OperacaoRequisicaoConcluida(e.eventId(),java.time.Instant.now(),e.organizacaoId(),1,e.requisicaoId(),e.eventId(),erro==null,erro));
 }
}
