package com.almoxarifado.requisicao.application;
import java.util.*;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import com.almoxarifado.requisicao.domain.Requisicao;
import com.almoxarifado.requisicao.events.OperacaoRequisicaoSolicitada;
import com.almoxarifado.shared.application.*;
import com.almoxarifado.shared.domain.RecursoNaoEncontradoException;
@Service @Transactional(readOnly=true)
@PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','REQUISITANTE','GESTOR')")
public class RequisicaoService {
 private final RequisicaoRepository repository;private final ContextoUsuario contexto;private final Auditoria auditoria;private final PublicadorEventos eventos;
 public RequisicaoService(RequisicaoRepository repository,ContextoUsuario contexto,Auditoria auditoria,PublicadorEventos eventos) {
  this.repository=repository;this.contexto=contexto;this.auditoria=auditoria;this.eventos=eventos;
 }
 private boolean restrito() { return contexto.possuiPerfil("REQUISITANTE") && !contexto.possuiPerfil("ADMIN_PLATAFORMA") && !contexto.possuiPerfil("ADMIN_ORGANIZACAO") && !contexto.possuiPerfil("ALMOXARIFE") && !contexto.possuiPerfil("GESTOR"); }
 private Requisicao buscar(UUID id,boolean lock) {
  var r=repository.buscar(contexto.organizacaoId(),id,lock).orElseThrow(()->new RecursoNaoEncontradoException("Requisicao nao encontrada"));
  if(restrito() && !r.solicitanteId().equals(contexto.usuarioId())) throw new RecursoNaoEncontradoException("Requisicao nao encontrada");
  exigirLocal(r.localId());
  return r;
 }
 public Requisicao buscar(UUID id) { return buscar(id,false); }
 public List<Requisicao> listar(int pagina,int tamanho) {
  if(pagina<0 || pagina>1000000 || tamanho<1 || tamanho>100) throw new IllegalArgumentException("Paginacao invalida");
  return repository.listar(contexto.organizacaoId(),restrito()?contexto.usuarioId():null,tamanho,pagina*tamanho,contexto.almoxarifadosPermitidos());
 }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','REQUISITANTE')")
 public Requisicao criar(UUID local,List<Requisicao.Linha> linhas) {
  return criar(local,linhas,null);
 }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','REQUISITANTE')")
 public Requisicao criar(UUID local,List<Requisicao.Linha> linhas,String setorCodigo) {
  exigirLocal(local);
  var r=comSetor(new Requisicao(UUID.randomUUID(),contexto.organizacaoId(),contexto.usuarioId(),local,linhas,"RASCUNHO",null,null),setorCodigo);
  repository.criar(r);auditoria.registrar(r.organizacaoId(),contexto.usuarioId(),"REQUISICAO_CRIADA",r.id().toString());return r;
 }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','REQUISITANTE')")
 public Requisicao editar(UUID id,UUID local,List<Requisicao.Linha> linhas) {
  return editar(id,local,linhas,null);
 }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','REQUISITANTE')")
 public Requisicao editar(UUID id,UUID local,List<Requisicao.Linha> linhas,String setorCodigo) {
  exigirLocal(local);
  var r=buscar(id,true);
  r.transicionar(r.status(),null,null,"RASCUNHO","FALHA_RESERVA");
  var novo=comSetor(new Requisicao(id,r.organizacaoId(),r.solicitanteId(),local,linhas,r.status(),null,null),setorCodigo);
  repository.editar(novo);auditoria.registrar(r.organizacaoId(),contexto.usuarioId(),"REQUISICAO_EDITADA",id.toString());return novo;
 }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','REQUISITANTE')")
 public Requisicao enviar(UUID id) { return solicitar(buscar(id,true),"RESERVA","RESERVANDO","RASCUNHO","FALHA_RESERVA"); }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE')")
 public Requisicao separar(UUID id) { return salvar(buscar(id,true).transicionar("SEPARADA",null,null,"RESERVADA")); }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE')")
 public Requisicao entregar(UUID id) { return entregar(id,null); }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE')")
 public Requisicao entregar(UUID id,List<Requisicao.Linha> selecionadas) { return entregar(id,selecionadas,null); }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE')")
 public Requisicao entregar(UUID id,List<Requisicao.Linha> selecionadas,UUID chave) {
  var r=buscar(id,true);
  if(selecionadas!=null && chave==null) throw new IllegalArgumentException("Entrega parcial exige Idempotency-Key");
  String assinatura=selecionadas==null?"INTEGRAL":selecionadas.stream().sorted(Comparator.comparing(Requisicao.Linha::itemId))
   .map(l->l.itemId()+":"+l.quantidade().stripTrailingZeros().toPlainString()).collect(java.util.stream.Collectors.joining(";"));
  if(chave!=null) {
   repository.bloquearChaveEntrega(r.organizacaoId(),chave);var anterior=repository.chaveEntrega(r.organizacaoId(),chave);
   if(anterior.isPresent()) {
    if(!anterior.get().requisicaoId().equals(id) || !anterior.get().assinatura().equals(assinatura)) throw new com.almoxarifado.shared.domain.RegraNegocioException("IDEMPOTENCIA_CONFLITO","Chave reutilizada com outro pedido ou conteudo");
    return r;
   }
  }
  r.exigirEstado("SEPARADA","PARCIALMENTE_ENTREGUE");
  if(selecionadas==null) selecionadas=r.linhas().stream().filter(l->l.pendente().signum()>0).map(l->new Requisicao.Linha(l.itemId(),l.pendente())).toList();
  if(selecionadas.isEmpty() || selecionadas.size()>100 || selecionadas.stream().map(Requisicao.Linha::itemId).distinct().count()!=selecionadas.size())
   throw new IllegalArgumentException("Selecione itens distintos para entrega");
  for(var linha:selecionadas) r.linhas().stream().filter(l->l.itemId().equals(linha.itemId())).findFirst()
   .orElseThrow(()->new RecursoNaoEncontradoException("Item fora da requisicao")).entregar(linha.quantidade());
  var comando=UUID.randomUUID();var novo=r.transicionar("ENTREGANDO",comando,null,"SEPARADA","PARCIALMENTE_ENTREGUE");
  if(chave!=null) repository.registrarChaveEntrega(r.organizacaoId(),chave,id,assinatura,comando);
  repository.prepararEntrega(r.organizacaoId(),id,comando,selecionadas);salvar(novo);
  eventos.publicar(new OperacaoRequisicaoSolicitada(comando,Instant.now(),r.organizacaoId(),2,id,contexto.usuarioId(),"ENTREGA",r.localId(),selecionadas.stream().map(l->new OperacaoRequisicaoSolicitada.Linha(l.itemId(),l.quantidade())).toList()));return novo;
 }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','REQUISITANTE')")
 public Requisicao confirmar(UUID id) {
  var r=buscar(id,true);
  if(!r.solicitanteId().equals(contexto.usuarioId()) && !contexto.possuiPerfil("ADMIN_PLATAFORMA") && !contexto.possuiPerfil("ADMIN_ORGANIZACAO")) throw new RecursoNaoEncontradoException("Requisicao nao encontrada");
  return salvar(r.transicionar("FINALIZADA",null,null,"ENTREGUE"));
 }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','REQUISITANTE')")
 public Requisicao cancelar(UUID id) {
  var r=buscar(id,true);
  if(Set.of("RASCUNHO","FALHA_RESERVA").contains(r.status())) return salvar(r.transicionar("CANCELADA",null,null,"RASCUNHO","FALHA_RESERVA"));
  return solicitar(r,"LIBERACAO","CANCELANDO","RESERVADA","SEPARADA","PARCIALMENTE_ENTREGUE");
 }
 private Requisicao solicitar(Requisicao r,String tipo,String estado,String... permitidos) {
  var comando=UUID.randomUUID();var novo=r.transicionar(estado,comando,null,permitidos);
  salvar(novo);eventos.publicar(new OperacaoRequisicaoSolicitada(comando,Instant.now(),r.organizacaoId(),2,r.id(),contexto.usuarioId(),tipo,r.localId(),r.linhas().stream().filter(l->l.pendente().signum()>0).map(l->new OperacaoRequisicaoSolicitada.Linha(l.itemId(),l.pendente())).toList()));
  return novo;
 }
 private Requisicao salvar(Requisicao r) { repository.atualizar(r);auditoria.registrar(r.organizacaoId(),contexto.usuarioId(),"REQUISICAO_"+r.status(),r.id().toString());return r; }
 private void exigirLocal(UUID local) {
  if(contexto.almoxarifadosPermitidos()!=null) contexto.exigirAlmoxarifado(repository.almoxarifadoDoLocal(contexto.organizacaoId(),local));
 }
 public List<RequisicaoRepository.Alocacao> alocacoes(UUID id) {buscar(id);return repository.alocacoes(contexto.organizacaoId(),id);}
 private Requisicao comSetor(Requisicao r,String codigo) {
  var setor=codigo==null || codigo.isBlank()?null:repository.setor(r.organizacaoId(),codigo).orElseThrow(()->new RecursoNaoEncontradoException("Setor nao cadastrado nesta organizacao ou ainda nao sincronizado"));
  return new Requisicao(r.id(),r.organizacaoId(),r.solicitanteId(),r.localId(),r.linhas(),r.status(),r.comandoId(),r.erro(),r.estadoAnterior(),setor==null?null:setor.codigo(),setor==null?"":setor.centroCusto());
 }
 private void periodo(java.time.LocalDate inicio,java.time.LocalDate fim) {
  if(inicio==null || fim==null || fim.isBefore(inicio) || java.time.temporal.ChronoUnit.DAYS.between(inicio,fim)>366 || fim.equals(java.time.LocalDate.MAX)) throw new IllegalArgumentException("Periodo invalido ou superior a 366 dias");
 }
 @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','GESTOR')")
 public List<Map<String,Object>> consumo(java.time.LocalDate inicio,java.time.LocalDate fim,int pagina,int tamanho) {
  periodo(inicio,fim);if(pagina<0 || pagina>1000000 || tamanho<1 || tamanho>100) throw new IllegalArgumentException("Paginacao invalida");
  return repository.consumo(contexto.organizacaoId(),inicio,fim,contexto.almoxarifadosPermitidos(),tamanho,pagina*tamanho);
 }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE','GESTOR')")
 public String exportarConsumo(java.time.LocalDate inicio,java.time.LocalDate fim) {
  periodo(inicio,fim);var org=contexto.organizacaoId();var linhas=repository.consumo(org,inicio,fim,contexto.almoxarifadosPermitidos(),10001,0);
  if(linhas.size()>10000) throw new com.almoxarifado.shared.domain.RegraNegocioException("LIMITE_EXPORTACAO","Reduza o periodo para exportar ate 10000 linhas");
  var csv=new StringBuilder("\ufeffsetor_codigo,centro_custo,item_id,quantidade,custo_total\r\n");
  for(var linha:linhas) csv.append(java.util.stream.Stream.of("setor_codigo","centro_custo","item_id","quantidade","custo_total").map(k->csv(linha.get(k))).collect(java.util.stream.Collectors.joining(","))).append("\r\n");
  auditoria.registrar(org,contexto.usuarioId(),"CONSUMO_EXPORTADO",inicio+":"+fim+":"+linhas.size());return csv.toString();
 }
 private String csv(Object valor) {
  String texto=valor==null?"":valor.toString();
  if(valor instanceof String && texto.stripLeading().matches("(?s)^[=+@-].*")) texto="'"+texto;
  return "\""+texto.replace("\"","\"\"")+"\"";
 }
 @Transactional @PreAuthorize("hasAnyRole('ADMIN_PLATAFORMA','ADMIN_ORGANIZACAO','ALMOXARIFE')")
 public Requisicao realocar(UUID id,UUID item,List<com.almoxarifado.requisicao.domain.AlocacaoReserva> alocacoes,String motivo,UUID chave) {
  if(chave==null || motivo==null || motivo.isBlank() || alocacoes==null || alocacoes.isEmpty() || alocacoes.size()>100) throw new IllegalArgumentException("Chave, motivo e alocacoes obrigatorios");
  var r=buscar(id,true);repository.bloquearChaveEntrega(r.organizacaoId(),chave);
  String assinatura=item+":"+motivo+":"+alocacoes.stream().sorted(Comparator.comparing(com.almoxarifado.requisicao.domain.AlocacaoReserva::posicaoId))
   .map(a->a.posicaoId()+":"+a.quantidade().stripTrailingZeros().toPlainString()).collect(java.util.stream.Collectors.joining(";"));
  var anterior=repository.chaveRealocacao(r.organizacaoId(),chave);
  if(anterior.isPresent()) {
   if(!anterior.get().requisicaoId().equals(id) || !anterior.get().assinatura().equals(assinatura)) throw new com.almoxarifado.shared.domain.RegraNegocioException("IDEMPOTENCIA_CONFLITO","Chave reutilizada com outro conteudo");
   return r;
  }
  r.exigirEstado("RESERVADA","SEPARADA","PARCIALMENTE_ENTREGUE");
  var linha=r.linhas().stream().filter(l->l.itemId().equals(item)).findFirst().orElseThrow(()->new RecursoNaoEncontradoException("Item fora do pedido"));
  var total=alocacoes.stream().map(com.almoxarifado.requisicao.domain.AlocacaoReserva::quantidade).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add);
  if(linha.pendente().signum()==0 || total.compareTo(linha.pendente())!=0 || alocacoes.stream().map(com.almoxarifado.requisicao.domain.AlocacaoReserva::posicaoId).distinct().count()!=alocacoes.size())
   throw new com.almoxarifado.shared.domain.RegraNegocioException("ALOCACAO_INVALIDA","Alocacoes devem preservar a quantidade restante do item");
  var comando=UUID.randomUUID();var novo=r.transicionar("REALOCANDO",comando,null,r.status());
  repository.registrarChaveRealocacao(r.organizacaoId(),chave,id,assinatura,comando);salvar(novo);
  eventos.publicar(new com.almoxarifado.requisicao.events.RealocacaoReservaSolicitada(comando,Instant.now(),r.organizacaoId(),1,id,item,r.localId(),contexto.usuarioId(),motivo,
   alocacoes.stream().map(a->new com.almoxarifado.requisicao.events.RealocacaoReservaSolicitada.Alocacao(a.posicaoId(),a.quantidade())).toList()));return novo;
 }
}
