package com.almoxarifado.requisicao.infrastructure.messaging;
import org.springframework.stereotype.Component;
import org.springframework.modulith.events.ApplicationModuleListener;
import com.almoxarifado.requisicao.events.OperacaoRequisicaoConcluida;
import com.almoxarifado.requisicao.application.RequisicaoRepository;
import com.almoxarifado.shared.application.Auditoria;
@Component
public class ResultadoEstoqueListener {
 private final RequisicaoRepository repository; private final Auditoria auditoria;
 public ResultadoEstoqueListener(RequisicaoRepository repository,Auditoria auditoria) { this.repository=repository;this.auditoria=auditoria; }
 @ApplicationModuleListener public void receber(OperacaoRequisicaoConcluida e) {
  var r=repository.buscar(e.organizacaoId(),e.requisicaoId(),true).orElseThrow();
  if(!e.comandoId().equals(r.comandoId())) return;
  String estado=switch(r.status()) {
   case "RESERVANDO" -> e.sucesso()?"RESERVADA":"FALHA_RESERVA";
   case "ENTREGANDO" -> e.sucesso()?"ENTREGUE":"SEPARADA";
   case "CANCELANDO" -> e.sucesso()?"CANCELADA":"RESERVADA";
   default -> null;
  };
  if(estado==null) return;
  repository.atualizar(r.transicionar(estado,null,e.erro(),r.status()));
  auditoria.registrar(e.organizacaoId(),"sistema","REQUISICAO_"+estado,r.id().toString());
 }
}
