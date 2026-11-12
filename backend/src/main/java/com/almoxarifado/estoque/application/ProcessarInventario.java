package com.almoxarifado.estoque.application;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import com.almoxarifado.shared.events.AjusteInventarioSolicitado;
import com.almoxarifado.shared.domain.RegraNegocioException;
@Service
public class ProcessarInventario {
 private final MotorEstoque motor;private final EstoqueRepository repository;
 public ProcessarInventario(MotorEstoque motor,EstoqueRepository repository) { this.motor=motor;this.repository=repository; }
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void executar(AjusteInventarioSolicitado e) {
  repository.bloquear(e.organizacaoId(),e.eventId());
  if(repository.processado(e.organizacaoId(),e.eventId())) return;
  for(var item:e.linhas().stream().map(AjusteInventarioSolicitado.Linha::itemId).distinct().sorted().toList()) repository.bloquear(e.organizacaoId(),item);
  for(var l:e.linhas()) {
   var p=repository.posicoes(e.organizacaoId(),l.itemId()).stream().filter(v->v.id().equals(l.posicaoId()) && v.localId().equals(e.localId())).findFirst().orElseThrow();
   if(repository.revisao(e.organizacaoId(),p.id())!=l.revisao()) throw new RegraNegocioException("ESTOQUE_ALTERADO","Estoque mudou durante a contagem; abra novo inventario");
  }
  for(var l:e.linhas().stream().sorted(Comparator.comparing(AjusteInventarioSolicitado.Linha::posicaoId)).toList()) {
   var delta=l.contada().subtract(l.sistema());if(delta.signum()==0) continue;
   var op=UUID.nameUUIDFromBytes((e.eventId()+":"+l.posicaoId()).getBytes(StandardCharsets.UTF_8));
   motor.executar(e.organizacaoId(),e.usuarioId(),op,new ComandoEstoque("AJUSTE",l.itemId(),null,e.localId(),delta,l.lote(),l.serie(),l.validade(),l.custo(),e.justificativa(),e.inventarioId()));
  }
  repository.concluirProcessamento(e.organizacaoId(),e.eventId());
 }
}
