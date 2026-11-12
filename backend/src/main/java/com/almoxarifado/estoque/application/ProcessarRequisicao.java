package com.almoxarifado.estoque.application;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import com.almoxarifado.requisicao.events.OperacaoRequisicaoSolicitada;
@Service
public class ProcessarRequisicao {
 private final MotorEstoque motor;
 public ProcessarRequisicao(MotorEstoque motor) { this.motor=motor; }
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void executar(OperacaoRequisicaoSolicitada e) {
  // Ordenacao uniforme evita deadlocks entre requisicoes com multiplos itens.
  for(var l:e.linhas().stream().sorted(java.util.Comparator.comparing(OperacaoRequisicaoSolicitada.Linha::itemId)).toList()) {
   UUID operacao=UUID.nameUUIDFromBytes((e.eventId()+":"+l.itemId()).getBytes(StandardCharsets.UTF_8));
   motor.executar(e.organizacaoId(),e.usuarioId(),operacao,new ComandoEstoque(e.operacao(),l.itemId(),e.localId(),null,l.quantidade(),null,null,null,null,"Requisicao "+e.requisicaoId(),e.requisicaoId()));
  }
 }
}
