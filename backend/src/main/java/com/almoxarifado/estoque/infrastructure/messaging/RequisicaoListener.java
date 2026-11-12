package com.almoxarifado.estoque.infrastructure.messaging;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.modulith.events.ApplicationModuleListener;
import com.almoxarifado.requisicao.events.OperacaoRequisicaoSolicitada;
import com.almoxarifado.requisicao.events.OperacaoRequisicaoConcluida;
import com.almoxarifado.estoque.application.ProcessarRequisicao;
import com.almoxarifado.shared.application.PublicadorEventos;
import com.almoxarifado.shared.domain.*;
@Component
public class RequisicaoListener {
 private final ProcessarRequisicao processar;private final PublicadorEventos eventos;
 public RequisicaoListener(ProcessarRequisicao processar,PublicadorEventos eventos) { this.processar=processar;this.eventos=eventos; }
 @ApplicationModuleListener public void receber(OperacaoRequisicaoSolicitada e) {
  String erro=null;
  try { processar.executar(e); }
  catch(RegraNegocioException ex) { erro=ex.getCodigo(); }
  catch(RecursoNaoEncontradoException ex) { erro="REFERENCIA_INDISPONIVEL"; }
  eventos.publicar(new OperacaoRequisicaoConcluida(e.eventId(),java.time.Instant.now(),e.organizacaoId(),1,e.requisicaoId(),e.eventId(),erro==null,erro));
 }
}
