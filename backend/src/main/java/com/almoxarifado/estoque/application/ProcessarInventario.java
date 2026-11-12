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
  repository.bloquearLocal(e.organizacaoId(),e.localId());repository.validarLocal(e.organizacaoId(),e.localId());
  var atuais=repository.posicoesDoLocal(e.organizacaoId(),e.localId());
  var itens=new TreeSet<UUID>();atuais.forEach(p->itens.add(p.itemId()));e.linhas().forEach(l->itens.add(l.itemId()));
  for(var item:itens) repository.bloquear(e.organizacaoId(),item);
  if(e.localCompleto()) {
   var esperadas=e.linhas().stream().filter(l->l.revisao()>0).map(AjusteInventarioSolicitado.Linha::posicaoId).collect(java.util.stream.Collectors.toSet());
   var existentes=atuais.stream().map(EstoqueRepository.PosicaoLocal::posicaoId).collect(java.util.stream.Collectors.toSet());
   if(!esperadas.equals(existentes)) throw new RegraNegocioException("ESTOQUE_ALTERADO","Posicoes foram criadas durante a contagem");
  }
  for(var l:e.linhas()) {
   if(l.revisao()==0) continue;
   var p=repository.posicoes(e.organizacaoId(),l.itemId()).stream().filter(v->v.id().equals(l.posicaoId()) && v.localId().equals(e.localId())).findFirst().orElseThrow(()->new RegraNegocioException("ESTOQUE_ALTERADO","Posicao alterada durante a contagem"));
   if(repository.revisao(e.organizacaoId(),p.id())!=l.revisao()) throw new RegraNegocioException("ESTOQUE_ALTERADO","Estoque mudou durante a contagem; abra novo inventario");
  }
  for(var l:e.linhas().stream().sorted(Comparator.comparing(AjusteInventarioSolicitado.Linha::posicaoId)).toList()) {
   var delta=l.contada().subtract(l.sistema());if(delta.signum()==0) continue;
   if(l.revisao()==0) repository.salvar(new com.almoxarifado.estoque.domain.PosicaoEstoque(l.posicaoId(),e.organizacaoId(),l.itemId(),e.localId(),l.lote(),l.serie(),l.validade(),java.time.Instant.now(),java.math.BigDecimal.ZERO,java.math.BigDecimal.ZERO,l.custo()));
   var op=UUID.nameUUIDFromBytes((e.eventId()+":"+l.posicaoId()).getBytes(StandardCharsets.UTF_8));
   motor.executar(e.organizacaoId(),e.usuarioId(),op,new ComandoEstoque("AJUSTE",l.itemId(),null,e.localId(),delta,l.lote(),l.serie(),l.validade(),l.custo(),e.justificativa(),e.inventarioId()));
  }
  repository.concluirProcessamento(e.organizacaoId(),e.eventId());
 }
}
