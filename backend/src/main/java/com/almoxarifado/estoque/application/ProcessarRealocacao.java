package com.almoxarifado.estoque.application;
import java.util.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import com.almoxarifado.requisicao.events.RealocacaoReservaSolicitada;
import com.almoxarifado.shared.domain.*;
@Service
public class ProcessarRealocacao {
 private final MotorEstoque motor;private final EstoqueRepository repository;
 public ProcessarRealocacao(MotorEstoque motor,EstoqueRepository repository) {this.motor=motor;this.repository=repository;}
 private UUID chave(UUID comando,String etapa) {return UUID.nameUUIDFromBytes((comando+":"+etapa).getBytes(StandardCharsets.UTF_8));}
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void executar(RealocacaoReservaSolicitada e) {
  repository.bloquear(e.organizacaoId(),e.eventId());if(repository.processado(e.organizacaoId(),e.eventId())) return;
  repository.bloquearLocal(e.organizacaoId(),e.localId());
  repository.bloquear(e.organizacaoId(),e.itemId());var posicoes=repository.posicoes(e.organizacaoId(),e.itemId());
  var reservas=repository.reservas(e.organizacaoId(),e.requisicaoId(),e.itemId());
  var total=reservas.stream().map(EstoqueRepository.Reserva::quantidade).reduce(BigDecimal.ZERO,BigDecimal::add);
  if(e.alocacoes().isEmpty() || e.alocacoes().stream().map(RealocacaoReservaSolicitada.Alocacao::posicaoId).distinct().count()!=e.alocacoes().size()
   || total.signum()==0 || e.alocacoes().stream().map(RealocacaoReservaSolicitada.Alocacao::quantidade).reduce(BigDecimal.ZERO,BigDecimal::add).compareTo(total)!=0)
   throw new RegraNegocioException("ALOCACAO_INVALIDA","A troca deve preservar toda a reserva restante do item");
  motor.executar(e.organizacaoId(),e.usuarioId(),chave(e.eventId(),"liberacao"),new ComandoEstoque("LIBERACAO",e.itemId(),e.localId(),null,null,null,null,null,null,e.motivo(),e.requisicaoId()));
  for(var a:e.alocacoes().stream().sorted(Comparator.comparing(RealocacaoReservaSolicitada.Alocacao::posicaoId)).toList()) {
   var p=posicoes.stream().filter(v->v.id().equals(a.posicaoId()) && v.localId().equals(e.localId())).findFirst()
    .orElseThrow(()->new RecursoNaoEncontradoException("Posicao fora do item, local ou organizacao"));
   if(a.quantidade()==null || a.quantidade().signum()<=0) throw new RegraNegocioException("ALOCACAO_INVALIDA","Quantidade invalida");
   motor.executar(e.organizacaoId(),e.usuarioId(),chave(e.eventId(),p.id().toString()),new ComandoEstoque("RESERVA",e.itemId(),e.localId(),null,a.quantidade(),p.lote(),p.serie(),null,null,e.motivo(),e.requisicaoId()));
  }
  repository.concluirProcessamento(e.organizacaoId(),e.eventId());
 }
}
