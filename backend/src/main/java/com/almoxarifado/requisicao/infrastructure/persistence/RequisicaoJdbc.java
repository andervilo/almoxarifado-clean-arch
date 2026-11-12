package com.almoxarifado.requisicao.infrastructure.persistence;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import com.almoxarifado.requisicao.domain.Requisicao;
import com.almoxarifado.requisicao.application.RequisicaoRepository;
@Repository
public class RequisicaoJdbc implements RequisicaoRepository {
 private final JdbcTemplate jdbc;
 public RequisicaoJdbc(JdbcTemplate jdbc) { this.jdbc=jdbc; }
 private RowMapper<Requisicao> mapper() {
  return (rs,n)->{
   var org=rs.getObject("organizacao_id",UUID.class);var id=rs.getObject("id",UUID.class);
   var linhas=jdbc.query("SELECT * FROM requisicao.linhas WHERE organizacao_id=? AND requisicao_id=? ORDER BY item_id",
    (r,i)->new Requisicao.Linha(r.getObject("item_id",UUID.class),r.getBigDecimal("quantidade"),r.getBigDecimal("atendida")),org,id);
   return new Requisicao(id,org,rs.getString("solicitante_id"),rs.getObject("local_id",UUID.class),linhas,rs.getString("status"),rs.getObject("comando_id",UUID.class),rs.getString("erro"),rs.getString("estado_anterior"),rs.getString("setor_codigo"),rs.getString("centro_custo"));
  };
 }
 public void criar(Requisicao r) {
  jdbc.update("INSERT INTO requisicao.requisicoes(organizacao_id,id,solicitante_id,local_id,status,setor_codigo,centro_custo) VALUES(?,?,?,?,?,?,?)",r.organizacaoId(),r.id(),r.solicitanteId(),r.localId(),r.status(),r.setorCodigo(),r.centroCusto());
  for(var l:r.linhas()) jdbc.update("INSERT INTO requisicao.linhas(organizacao_id,requisicao_id,item_id,quantidade) VALUES(?,?,?,?)",r.organizacaoId(),r.id(),l.itemId(),l.quantidade());
 }
 public Optional<Requisicao> buscar(UUID org,UUID id,boolean bloquear) {
  return jdbc.query("SELECT * FROM requisicao.requisicoes WHERE organizacao_id=? AND id=?"+(bloquear?" FOR UPDATE":""),mapper(),org,id).stream().findFirst();
 }
 public List<Requisicao> listar(UUID org,String solicitante,int limite,int deslocamento,Set<UUID> escopo) {
  String ids=com.almoxarifado.shared.infrastructure.persistence.FiltroEscopoSql.array(escopo);
  return jdbc.query("""
   SELECT r.* FROM requisicao.requisicoes r LEFT JOIN requisicao.locais_projecao l ON l.organizacao_id=r.organizacao_id AND l.id=r.local_id
   WHERE r.organizacao_id=? AND (?::text IS NULL OR r.solicitante_id=?) AND (?::uuid[] IS NULL OR l.almoxarifado_id=ANY(?::uuid[]))
   ORDER BY r.criado_em DESC,r.id LIMIT ? OFFSET ?
   """,mapper(),org,solicitante,solicitante,ids,ids,limite,deslocamento);
 }
 public void atualizar(Requisicao r) {
  jdbc.update("UPDATE requisicao.requisicoes SET status=?,comando_id=?,erro=?,estado_anterior=? WHERE organizacao_id=? AND id=?",r.status(),r.comandoId(),r.erro(),r.estadoAnterior(),r.organizacaoId(),r.id());
 }
 public void editar(Requisicao r) {
  jdbc.update("UPDATE requisicao.requisicoes SET local_id=?,erro=NULL,setor_codigo=?,centro_custo=? WHERE organizacao_id=? AND id=?",r.localId(),r.setorCodigo(),r.centroCusto(),r.organizacaoId(),r.id());
  jdbc.update("DELETE FROM requisicao.linhas WHERE organizacao_id=? AND requisicao_id=?",r.organizacaoId(),r.id());
  for(var l:r.linhas()) jdbc.update("INSERT INTO requisicao.linhas(organizacao_id,requisicao_id,item_id,quantidade) VALUES(?,?,?,?)",r.organizacaoId(),r.id(),l.itemId(),l.quantidade());
 }
 public UUID almoxarifadoDoLocal(UUID org,UUID local) {
  return jdbc.query("SELECT almoxarifado_id FROM requisicao.locais_projecao WHERE organizacao_id=? AND id=?",(rs,n)->rs.getObject(1,UUID.class),org,local).stream().findFirst().orElse(null);
 }
 public void prepararEntrega(UUID org,UUID req,UUID comando,List<Requisicao.Linha> linhas) {
  for(var l:linhas) jdbc.update("INSERT INTO requisicao.entregas_pendentes(organizacao_id,requisicao_id,comando_id,item_id,quantidade) VALUES(?,?,?,?,?)",org,req,comando,l.itemId(),l.quantidade());
 }
 public void aplicarEntrega(UUID org,UUID req,UUID comando) {
  int linhas=jdbc.queryForObject("SELECT count(*) FROM requisicao.entregas_pendentes WHERE organizacao_id=? AND requisicao_id=? AND comando_id=?",Integer.class,org,req,comando);
  if(linhas==0) {
   // Compatibilidade com comandos de entrega integral publicados antes desta versao.
   jdbc.update("UPDATE requisicao.linhas SET atendida=quantidade WHERE organizacao_id=? AND requisicao_id=?",org,req);
  } else {
   jdbc.update("""
    UPDATE requisicao.linhas l SET atendida=l.atendida+e.quantidade FROM requisicao.entregas_pendentes e
    WHERE e.organizacao_id=? AND e.requisicao_id=? AND e.comando_id=? AND NOT e.confirmada
     AND l.organizacao_id=e.organizacao_id AND l.requisicao_id=e.requisicao_id AND l.item_id=e.item_id
    """,org,req,comando);
   jdbc.update("UPDATE requisicao.entregas_pendentes SET confirmada=TRUE WHERE organizacao_id=? AND requisicao_id=? AND comando_id=?",org,req,comando);
  }
 }
 public void bloquearChaveEntrega(UUID org,UUID chave) {
  jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",Object.class,"entrega:"+org+":"+chave);
 }
 public Optional<ChaveEntrega> chaveEntrega(UUID org,UUID chave) {
  return jdbc.query("SELECT * FROM requisicao.chaves_entrega WHERE organizacao_id=? AND chave=?",(rs,n)->new ChaveEntrega(rs.getObject("requisicao_id",UUID.class),rs.getString("assinatura"),rs.getObject("comando_id",UUID.class)),org,chave).stream().findFirst();
 }
 public void registrarChaveEntrega(UUID org,UUID chave,UUID req,String assinatura,UUID comando) {
  jdbc.update("INSERT INTO requisicao.chaves_entrega(organizacao_id,chave,requisicao_id,assinatura,comando_id) VALUES(?,?,?,?,?)",org,chave,req,assinatura,comando);
 }
 public List<Alocacao> alocacoes(UUID org,UUID req) {
  return jdbc.query("SELECT * FROM requisicao.alocacoes WHERE organizacao_id=? AND requisicao_id=? ORDER BY item_id,posicao_id",(rs,n)->new Alocacao(rs.getObject("item_id",UUID.class),rs.getObject("posicao_id",UUID.class),rs.getObject("local_id",UUID.class),rs.getString("lote"),rs.getString("serie"),rs.getObject("validade",java.time.LocalDate.class),rs.getBigDecimal("quantidade")),org,req);
 }
 public Optional<ChaveEntrega> chaveRealocacao(UUID org,UUID chave) {
  return jdbc.query("SELECT * FROM requisicao.chaves_realocacao WHERE organizacao_id=? AND chave=?",(rs,n)->new ChaveEntrega(rs.getObject("requisicao_id",UUID.class),rs.getString("assinatura"),rs.getObject("comando_id",UUID.class)),org,chave).stream().findFirst();
 }
 public void registrarChaveRealocacao(UUID org,UUID chave,UUID req,String assinatura,UUID comando) {
  jdbc.update("INSERT INTO requisicao.chaves_realocacao(organizacao_id,chave,requisicao_id,assinatura,comando_id) VALUES(?,?,?,?,?)",org,chave,req,assinatura,comando);
 }
 public Optional<SetorReferencia> setor(UUID org,String codigo) {
  return jdbc.query("SELECT * FROM requisicao.setores_projecao WHERE organizacao_id=? AND codigo=?",(rs,n)->new SetorReferencia(codigo,rs.getString("nome"),rs.getString("centro_custo")),org,codigo).stream().findFirst();
 }
 public List<Map<String,Object>> consumo(UUID org,java.time.LocalDate inicio,java.time.LocalDate fim,Set<UUID> escopo,int limite,int deslocamento) {
  String ids=com.almoxarifado.shared.infrastructure.persistence.FiltroEscopoSql.array(escopo);
  return jdbc.queryForList("""
   SELECT r.setor_codigo,r.centro_custo,c.item_id,sum(c.quantidade) AS quantidade,sum(c.custo_total) AS custo_total
   FROM requisicao.consumo c JOIN requisicao.requisicoes r ON r.organizacao_id=c.organizacao_id AND r.id=c.requisicao_id
   LEFT JOIN requisicao.locais_projecao l ON l.organizacao_id=r.organizacao_id AND l.id=r.local_id
   WHERE c.organizacao_id=? AND c.efetivado_em>=? AND c.efetivado_em<? AND (?::uuid[] IS NULL OR l.almoxarifado_id=ANY(?::uuid[]))
   GROUP BY r.setor_codigo,r.centro_custo,c.item_id ORDER BY r.setor_codigo NULLS LAST,r.centro_custo,c.item_id LIMIT ? OFFSET ?
   """,org,java.sql.Timestamp.from(inicio.atStartOfDay(java.time.ZoneOffset.UTC).toInstant()),java.sql.Timestamp.from(fim.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant()),ids,ids,limite,deslocamento);
 }
}
