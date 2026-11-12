package com.almoxarifado.organizacao.application;
import java.util.UUID;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.almoxarifado.organizacao.domain.*;
import com.almoxarifado.organizacao.events.ProvisionamentoIdentidadeSolicitado;
import com.almoxarifado.shared.application.*;
import com.almoxarifado.shared.domain.RegraNegocioException;
@Service
public class SincronizarIdentidade {
 private final ProvisionamentoRepository repository;private final OrganizacaoRepository organizacoes;private final ProvedorIdentidade provedor;private final PublicadorEventos eventos;private final Auditoria auditoria;
 public SincronizarIdentidade(ProvisionamentoRepository repository,OrganizacaoRepository organizacoes,ProvedorIdentidade provedor,PublicadorEventos eventos,Auditoria auditoria) {this.repository=repository;this.organizacoes=organizacoes;this.provedor=provedor;this.eventos=eventos;this.auditoria=auditoria;}
 @Transactional public void executar(UUID org,UUID id) {
  var p=repository.buscar(org,id,true).orElseThrow();if(java.util.Set.of("CONCLUIDO","FALHA","SUPERADO").contains(p.status())) return;
  try {
   if(!organizacoes.buscar(org).orElseThrow().isAtiva()) throw new RegraNegocioException("ORGANIZACAO_INATIVA","Organizacao inativa");
   if(p.status().equals("ATIVANDO")) {
    repository.bloquearUsuario(org,p.keycloakId());
    if(!repository.atual(org,p.keycloakId(),id)) {repository.estado(org,id,"SUPERADO",p.keycloakId(),null);return;}
    provedor.definirAtivo(org,p.keycloakId(),p.dados().ativo());repository.estado(org,id,"CONCLUIDO",p.keycloakId(),null);
    auditoria.registrar(org,p.ator(),"IDENTIDADE_CONCLUIDA",id.toString());
   } else {
    if(p.dados().keycloakId()!=null) {
     repository.bloquearUsuario(org,p.dados().keycloakId());
     if(repository.possuiPosterior(org,p.dados().keycloakId(),id)) {repository.estado(org,id,"SUPERADO",p.dados().keycloakId(),null);return;}
    }
    UUID externo=provedor.preparar(org,id,p.dados());
    repository.bloquearUsuario(org,externo);repository.marcarAtual(org,externo,id);
    var usuario=new Usuario(externo.toString(),p.dados().nome(),p.dados().email(),p.dados().perfil(),org.toString());if(!p.dados().ativo()) usuario.inativar();
    organizacoes.salvarUsuario(org,usuario);repository.estado(org,id,"ATIVANDO",externo,null);
    eventos.publicar(new ProvisionamentoIdentidadeSolicitado(UUID.randomUUID(),Instant.now(),org,1,id));
    auditoria.registrar(org,p.ator(),"IDENTIDADE_PREPARADA",id.toString());
   }
  } catch(RegraNegocioException ex) {
   if(p.status().equals("ATIVANDO") && p.keycloakId()!=null) organizacoes.usuarios(org).stream().filter(u->u.getCodigo().equals(p.keycloakId().toString())).findFirst().ifPresent(u->{u.inativar();organizacoes.salvarUsuario(org,u);});
   repository.estado(org,id,"FALHA",p.keycloakId(),ex.getCodigo());auditoria.registrar(org,p.ator(),"IDENTIDADE_FALHOU",id.toString());
  }
 }
}
