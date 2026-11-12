package com.almoxarifado.catalogo.application;
import java.util.*;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import com.almoxarifado.catalogo.domain.*;
import com.almoxarifado.catalogo.events.ItemAlterado;
import com.almoxarifado.shared.application.*;
import com.almoxarifado.shared.domain.*;
@Service
@Transactional(readOnly=true)
@PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','GESTOR','REQUISITANTE')")
public class CatalogoService {
 private final CatalogoRepository repository;
 private final ContextoUsuario contexto;
 private final Auditoria auditoria;
 private final PublicadorEventos eventos;
 public CatalogoService(CatalogoRepository repository,ContextoUsuario contexto,Auditoria auditoria,PublicadorEventos eventos) {
  this.repository=repository; this.contexto=contexto; this.auditoria=auditoria; this.eventos=eventos;
 }
 private UUID tenant() {
  var org=contexto.organizacaoId();
  if(!repository.organizacaoAtiva(org)) throw new RegraNegocioException("ORGANIZACAO_INDISPONIVEL","Organizacao inativa ou ainda nao sincronizada");
  return org;
 }
 private void pagina(int pagina,int tamanho) {
  if(pagina<0 || tamanho<1 || tamanho>100 || pagina>1000000) throw new IllegalArgumentException("Paginacao invalida");
 }
 public Categoria buscarCategoria(UUID id) { return repository.buscarCategoria(tenant(),id).orElseThrow(()->new RecursoNaoEncontradoException("Categoria nao encontrado")); }
 public List<Categoria> listarCategoria(int pagina,int tamanho) { pagina(pagina,tamanho); return repository.listarCategoria(tenant(),tamanho,pagina*tamanho); }
 @Transactional
 @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE')")
 public Categoria salvarCategoria(UUID id,boolean atualizar,Categoria dados) {
  var org=tenant();
  if(!org.equals(dados.organizacaoId())) throw new IllegalArgumentException("Organizacao invalida");
  if(atualizar) buscarCategoria(id);
  repository.salvar(dados);
  auditoria.registrar(org,contexto.usuarioId(),"CATEGORIA_SALVO",id.toString());
  return dados;
 }
 public UnidadeMedida buscarUnidadeMedida(UUID id) { return repository.buscarUnidadeMedida(tenant(),id).orElseThrow(()->new RecursoNaoEncontradoException("UnidadeMedida nao encontrado")); }
 public List<UnidadeMedida> listarUnidadeMedida(int pagina,int tamanho) { pagina(pagina,tamanho); return repository.listarUnidadeMedida(tenant(),tamanho,pagina*tamanho); }
 @Transactional
 @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE')")
 public UnidadeMedida salvarUnidadeMedida(UUID id,boolean atualizar,UnidadeMedida dados) {
  var org=tenant();
  if(!org.equals(dados.organizacaoId())) throw new IllegalArgumentException("Organizacao invalida");
  if(atualizar) buscarUnidadeMedida(id);
  repository.salvar(dados);
  auditoria.registrar(org,contexto.usuarioId(),"UNIDADEMEDIDA_SALVO",id.toString());
  return dados;
 }
 public Fornecedor buscarFornecedor(UUID id) { return repository.buscarFornecedor(tenant(),id).orElseThrow(()->new RecursoNaoEncontradoException("Fornecedor nao encontrado")); }
 public List<Fornecedor> listarFornecedor(int pagina,int tamanho) { pagina(pagina,tamanho); return repository.listarFornecedor(tenant(),tamanho,pagina*tamanho); }
 @Transactional
 @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE')")
 public Fornecedor salvarFornecedor(UUID id,boolean atualizar,Fornecedor dados) {
  var org=tenant();
  if(!org.equals(dados.organizacaoId())) throw new IllegalArgumentException("Organizacao invalida");
  if(atualizar) buscarFornecedor(id);
  repository.salvar(dados);
  auditoria.registrar(org,contexto.usuarioId(),"FORNECEDOR_SALVO",id.toString());
  return dados;
 }
 public Item buscarItem(UUID id) { return repository.buscarItem(tenant(),id).orElseThrow(()->new RecursoNaoEncontradoException("Item nao encontrado")); }
 public List<Item> listarItem(int pagina,int tamanho) { pagina(pagina,tamanho); return repository.listarItem(tenant(),tamanho,pagina*tamanho); }
 @Transactional
 @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE')")
 public Item salvarItem(UUID id,boolean atualizar,Item dados) {
  var org=tenant();
  if(!org.equals(dados.organizacaoId())) throw new IllegalArgumentException("Organizacao invalida");
  if(atualizar) buscarItem(id);

  if(!buscarCategoria(dados.categoriaId()).ativo() || !buscarUnidadeMedida(dados.unidadeMedidaId()).ativo())
   throw new RegraNegocioException("CADASTRO_INATIVO","Categoria ou unidade inativa");
  if(atualizar && buscarItem(id).rastreabilidade()!=dados.rastreabilidade())
   throw new RegraNegocioException("RASTREABILIDADE_IMUTAVEL","Crie outro item para alterar a rastreabilidade");
  repository.salvar(dados);
  auditoria.registrar(org,contexto.usuarioId(),"ITEM_SALVO",id.toString());
  eventos.publicar(new ItemAlterado(UUID.randomUUID(),Instant.now(),org,1,id,dados.codigo(),dados.descricao(),dados.rastreabilidade().name(),dados.estoqueMinimo(),dados.estoqueMaximo(),dados.ativo()));
  return dados;
 }
 public UUID organizacaoId() { return tenant(); }
}
