package com.almoxarifado.estoque.infrastructure.persistence;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import com.almoxarifado.estoque.application.*;
import com.almoxarifado.estoque.domain.PosicaoEstoque;
import com.almoxarifado.shared.domain.*;
@Repository
public class EstoqueJdbc implements EstoqueRepository {
 private final JdbcTemplate jdbc;
 public EstoqueJdbc(JdbcTemplate jdbc) { this.jdbc=jdbc; }
 public void bloquear(UUID org,UUID chave) { jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,org+":"+chave); }
 public ItemOperacional validarItem(UUID org,UUID item) {
  return jdbc.query("""
   SELECT i.* FROM estoque.itens_projecao i JOIN estoque.organizacoes_projecao o ON o.id=i.organizacao_id
   WHERE i.organizacao_id=? AND i.id=? AND i.ativo AND o.ativa
   """,(rs,n)->new ItemOperacional(rs.getString("rastreabilidade"),rs.getBigDecimal("estoque_minimo")),org,item)
   .stream().findFirst().orElseThrow(()->new RegraNegocioException("ITEM_INDISPONIVEL","Item ou organizacao inativo, inexistente ou nao sincronizado"));
 }
 public UUID validarLocal(UUID org,UUID local) {
  return jdbc.query("""
   SELECT l.almoxarifado_id FROM estoque.locais_projecao l JOIN estoque.almoxarifados_projecao a
   ON a.organizacao_id=l.organizacao_id AND a.id=l.almoxarifado_id
   WHERE l.organizacao_id=? AND l.id=? AND l.ativo AND a.ativo
   """,(rs,n)->rs.getObject(1,UUID.class),org,local).stream().findFirst()
   .orElseThrow(()->new RecursoNaoEncontradoException("Local ativo nao encontrado"));
 }
 public List<PosicaoEstoque> posicoes(UUID org,UUID item) {
  return jdbc.query("""
   SELECT * FROM estoque.posicoes WHERE organizacao_id=? AND item_id=?
   ORDER BY validade ASC NULLS LAST,recebido_em,id
   """,(rs,n)->new PosicaoEstoque(rs.getObject("id",UUID.class),org,item,rs.getObject("local_id",UUID.class),
     rs.getString("lote"),rs.getString("serie"),rs.getObject("validade",java.time.LocalDate.class),rs.getTimestamp("recebido_em").toInstant(),
     rs.getBigDecimal("quantidade"),rs.getBigDecimal("reservado"),rs.getBigDecimal("custo")),org,item);
 }
 public void salvar(PosicaoEstoque v) {
  jdbc.update("""
   INSERT INTO estoque.posicoes(organizacao_id,id,item_id,local_id,lote,serie,validade,recebido_em,quantidade,reservado,custo)
   VALUES(?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(organizacao_id,id) DO UPDATE SET quantidade=excluded.quantidade,reservado=excluded.reservado,custo=excluded.custo,revisao=estoque.posicoes.revisao+1
   """,v.organizacaoId(),v.id(),v.itemId(),v.localId(),v.lote(),v.serie(),v.validade(),java.sql.Timestamp.from(v.recebidoEm()),v.quantidade(),v.reservado(),v.custo());
 }
 public Optional<String> assinatura(UUID org,UUID id) {
  return jdbc.query("SELECT assinatura FROM estoque.operacoes WHERE organizacao_id=? AND id=?",(rs,n)->rs.getString(1),org,id).stream().findFirst();
 }
 public void operacao(UUID org,UUID id,ComandoEstoque c,String usuario,List<Linha> linhas) {
  jdbc.update("INSERT INTO estoque.operacoes(organizacao_id,id,item_id,tipo,assinatura,usuario_id,motivo) VALUES(?,?,?,?,?,?,?)",org,id,c.itemId(),c.tipo(),c.toString(),usuario,c.motivo());
  for(var l:linhas) jdbc.update("INSERT INTO estoque.linhas_operacao(organizacao_id,operacao_id,posicao_id,quantidade,custo) VALUES(?,?,?,?,?)",org,id,l.posicaoId(),l.quantidade(),l.custo());
 }
 public List<Linha> linhas(UUID org,UUID operacao) {
  return jdbc.query("SELECT * FROM estoque.linhas_operacao WHERE organizacao_id=? AND operacao_id=? ORDER BY id",(rs,n)->new Linha(rs.getObject("posicao_id",UUID.class),rs.getBigDecimal("quantidade"),rs.getBigDecimal("custo")),org,operacao);
 }
 public List<Map<String,Object>> historico(UUID org,UUID item,int limite,int deslocamento,Set<UUID> escopo) {
  String ids=com.almoxarifado.shared.infrastructure.persistence.FiltroEscopoSql.array(escopo);
  return jdbc.queryForList("""
   SELECT o.id,o.item_id,o.tipo,o.usuario_id,o.motivo,o.criado_em FROM estoque.operacoes o
   WHERE o.organizacao_id=? AND o.item_id=? AND (?::uuid[] IS NULL OR EXISTS(
    SELECT 1 FROM estoque.linhas_operacao l JOIN estoque.posicoes p ON p.organizacao_id=l.organizacao_id AND p.id=l.posicao_id
    JOIN estoque.locais_projecao lp ON lp.organizacao_id=p.organizacao_id AND lp.id=p.local_id
    WHERE l.organizacao_id=o.organizacao_id AND l.operacao_id=o.id AND lp.almoxarifado_id=ANY(?::uuid[])))
   ORDER BY o.criado_em DESC,o.id LIMIT ? OFFSET ?
   """,org,item,ids,ids,limite,deslocamento);
 }
 public List<Reserva> reservas(UUID org,UUID req,UUID item) {
  return jdbc.query("""
   SELECT r.posicao_id,r.quantidade FROM estoque.reservas r JOIN estoque.posicoes p ON p.organizacao_id=r.organizacao_id AND p.id=r.posicao_id
   WHERE r.organizacao_id=? AND r.requisicao_id=? AND p.item_id=? AND r.status='ATIVA'
   ORDER BY p.validade ASC NULLS LAST,p.recebido_em,p.id
   """,(rs,n)->new Reserva(rs.getObject("posicao_id",UUID.class),rs.getBigDecimal("quantidade")),org,req,item);
 }
 public void reservar(UUID org,UUID req,UUID pos,BigDecimal quantidade) {
  jdbc.update("INSERT INTO estoque.reservas(organizacao_id,requisicao_id,posicao_id,quantidade,status) VALUES(?,?,?,?,'ATIVA')",org,req,pos,quantidade);
 }
 public void concluirReserva(UUID org,UUID req,UUID item,String status) {
  jdbc.update("""
   UPDATE estoque.reservas r SET status=? WHERE r.organizacao_id=? AND r.requisicao_id=? AND r.status='ATIVA'
   AND EXISTS(SELECT 1 FROM estoque.posicoes p WHERE p.organizacao_id=r.organizacao_id AND p.id=r.posicao_id AND p.item_id=?)
   """,status,org,req,item);
 }
 public BigDecimal custo(UUID org,UUID item,UUID almox) {
  return jdbc.query("SELECT custo FROM estoque.custos WHERE organizacao_id=? AND item_id=? AND almoxarifado_id=?",(rs,n)->rs.getBigDecimal(1),org,item,almox).stream().findFirst().orElse(BigDecimal.ZERO);
 }
 public BigDecimal ajustarCusto(UUID org,UUID item,UUID almox,BigDecimal delta,BigDecimal valor) {
  var dados=jdbc.queryForList("SELECT quantidade,custo FROM estoque.custos WHERE organizacao_id=? AND item_id=? AND almoxarifado_id=?",org,item,almox);
  BigDecimal q=dados.isEmpty()?BigDecimal.ZERO:(BigDecimal)dados.getFirst().get("quantidade");
  BigDecimal c=dados.isEmpty()?BigDecimal.ZERO:(BigDecimal)dados.getFirst().get("custo");
  BigDecimal novo=q.add(delta);
  if(novo.signum()<0) throw new RegraNegocioException("SALDO_INSUFICIENTE","Saldo insuficiente no almoxarifado");
  BigDecimal custo=novo.signum()==0?BigDecimal.ZERO:(delta.signum()>0?q.multiply(c).add(delta.multiply(valor)).divide(novo,6,java.math.RoundingMode.HALF_EVEN):c);
  jdbc.update("""
   INSERT INTO estoque.custos(organizacao_id,item_id,almoxarifado_id,quantidade,custo) VALUES(?,?,?,?,?)
   ON CONFLICT(organizacao_id,item_id,almoxarifado_id) DO UPDATE SET quantidade=excluded.quantidade,custo=excluded.custo
   """,org,item,almox,novo,custo);
  return delta.signum()>0?valor:c;
 }
 public void marcarEstorno(UUID org,UUID original,UUID estorno) {
  jdbc.update("INSERT INTO estoque.estornos(organizacao_id,original_id,estorno_id) VALUES(?,?,?)",org,original,estorno);
 }
 public boolean estornado(UUID org,UUID original) {
  return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM estoque.estornos WHERE organizacao_id=? AND original_id=?)",Boolean.class,org,original));
 }
 public long revisao(UUID org,UUID posicao) {
  return jdbc.queryForObject("SELECT revisao FROM estoque.posicoes WHERE organizacao_id=? AND id=?",Long.class,org,posicao);
 }
 public boolean processado(UUID org,UUID evento) {
  return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM estoque.processamentos_integracao WHERE organizacao_id=? AND evento_id=?)",Boolean.class,org,evento));
 }
 public void concluirProcessamento(UUID org,UUID evento) {
  jdbc.update("INSERT INTO estoque.processamentos_integracao(organizacao_id,evento_id) VALUES(?,?)",org,evento);
 }
 public List<Map<String,Object>> rastrear(UUID org,UUID item,String lote,String serie,int limite,int deslocamento,Set<UUID> escopo) {
  String ids=com.almoxarifado.shared.infrastructure.persistence.FiltroEscopoSql.array(escopo);
  return jdbc.queryForList("""
   SELECT o.id AS operacao_id,o.tipo,o.usuario_id,o.motivo,o.criado_em,p.id AS posicao_id,p.local_id,p.lote,p.serie,p.validade,l.quantidade,l.custo
   FROM estoque.operacoes o JOIN estoque.linhas_operacao l ON l.organizacao_id=o.organizacao_id AND l.operacao_id=o.id
   JOIN estoque.posicoes p ON p.organizacao_id=l.organizacao_id AND p.id=l.posicao_id
   LEFT JOIN estoque.locais_projecao lp ON lp.organizacao_id=p.organizacao_id AND lp.id=p.local_id
   WHERE o.organizacao_id=? AND o.item_id=? AND (?::text IS NULL OR p.lote=?) AND (?::text IS NULL OR p.serie=?)
   AND (?::uuid[] IS NULL OR lp.almoxarifado_id=ANY(?::uuid[]))
   ORDER BY o.criado_em DESC,l.id LIMIT ? OFFSET ?
   """,org,item,lote,lote,serie,serie,ids,ids,limite,deslocamento);
 }
 public List<Map<String,Object>> custos(UUID org,UUID item,Set<UUID> escopo) {
  String ids=com.almoxarifado.shared.infrastructure.persistence.FiltroEscopoSql.array(escopo);
  return jdbc.queryForList("SELECT almoxarifado_id,quantidade,custo FROM estoque.custos WHERE organizacao_id=? AND item_id=? AND (?::uuid[] IS NULL OR almoxarifado_id=ANY(?::uuid[])) ORDER BY almoxarifado_id",org,item,ids,ids);
 }
 public UUID almoxarifadoDoLocal(UUID org,UUID local) {
  return jdbc.query("SELECT almoxarifado_id FROM estoque.locais_projecao WHERE organizacao_id=? AND id=?",(rs,n)->rs.getObject(1,UUID.class),org,local).stream().findFirst().orElse(null);
 }
}
