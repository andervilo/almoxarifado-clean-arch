package com.almoxarifado.alerta.application;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import com.almoxarifado.alerta.domain.Alerta;
import com.almoxarifado.shared.application.*;
import com.almoxarifado.shared.domain.RecursoNaoEncontradoException;
@Service @Transactional(readOnly=true)
@PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','GESTOR')")
public class AlertaService {
 private final AlertaRepository repository;private final ContextoUsuario contexto;private final Auditoria auditoria;
 public AlertaService(AlertaRepository repository,ContextoUsuario contexto,Auditoria auditoria) { this.repository=repository;this.contexto=contexto;this.auditoria=auditoria; }
 public List<Alerta> listar(int pagina,int tamanho) {
  if(pagina<0 || pagina>1000000 || tamanho<1 || tamanho>100) throw new IllegalArgumentException("Paginacao invalida");
  return repository.listar(contexto.organizacaoId(),tamanho,pagina*tamanho,contexto.almoxarifadosPermitidos());
 }
 public Map<String,Object> dashboard() { return repository.dashboard(contexto.organizacaoId(),contexto.almoxarifadosPermitidos()); }
 @Transactional public void dispensar(String chave,String revisao) {
  var org=contexto.organizacaoId();repository.buscar(org,chave,revisao,contexto.almoxarifadosPermitidos()).orElseThrow(()->new RecursoNaoEncontradoException("Alerta inexistente, desatualizado ou ja dispensado"));
  repository.dispensar(org,chave,revisao,contexto.usuarioId());auditoria.registrar(org,contexto.usuarioId(),"ALERTA_DISPENSADO",chave);
 }
}
