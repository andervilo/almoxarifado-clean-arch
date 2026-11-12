package com.almoxarifado.estrutura.application;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import com.almoxarifado.shared.application.*;
@Service
public class RepublicarEstrutura {
 private final SnapshotRepository snapshots;
 private final ContextoUsuario usuario;
 private final PublicadorEventos publicador;
 private final Auditoria auditoria;
 public RepublicarEstrutura(SnapshotRepository snapshots,ContextoUsuario usuario,PublicadorEventos publicador,Auditoria auditoria) {
  this.snapshots=snapshots;this.usuario=usuario;this.publicador=publicador;this.auditoria=auditoria;
 }
 public record Resultado(int registros,int eventos,UUID proximoCursor,boolean concluido) {}
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO')")
 public Resultado executar(String recurso,UUID apos,int tamanho) {
  if(tamanho<1 || tamanho>100) throw new IllegalArgumentException("Tamanho deve estar entre 1 e 100");
  var org=usuario.organizacaoId();var lote=snapshots.carregar(org,recurso,apos,tamanho);
  for(var snapshot:lote) for(var evento:snapshot.eventos()) publicador.publicar(evento);
  int eventos=lote.stream().mapToInt(s->s.eventos().size()).sum();
  auditoria.registrar(org,usuario.usuarioId(),"REPUBLICACAO_ESTRUTURA",recurso+":"+lote.size());
  return new Resultado(lote.size(),eventos,lote.isEmpty()?apos:lote.getLast().id(),lote.size()<tamanho);
 }
}
