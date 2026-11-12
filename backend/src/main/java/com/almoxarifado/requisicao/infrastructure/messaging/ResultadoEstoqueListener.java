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
  if(r.status().equals("ENTREGANDO") && e.sucesso()) {
   repository.aplicarEntrega(e.organizacaoId(),r.id(),e.comandoId());
   r=repository.buscar(e.organizacaoId(),r.id(),true).orElseThrow();
  }
  String estado=switch(r.status()) {
   case "RESERVANDO" -> e.sucesso()?"RESERVADA":"FALHA_RESERVA";
   case "ENTREGANDO" -> e.sucesso()?(r.linhas().stream().allMatch(l->l.pendente().signum()==0)?"ENTREGUE":"PARCIALMENTE_ENTREGUE"):(r.estadoAnterior()==null?"SEPARADA":r.estadoAnterior());
   case "CANCELANDO" -> e.sucesso()?"CANCELADA":(r.estadoAnterior()==null?"RESERVADA":r.estadoAnterior());
   default -> null;
  };
  if(estado==null) return;
  repository.atualizar(r.transicionar(estado,null,e.erro(),r.status()));
  auditoria.registrar(e.organizacaoId(),"sistema","REQUISICAO_"+estado,r.id().toString());
 }
}
