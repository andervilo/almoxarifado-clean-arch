package com.almoxarifado.alerta.infrastructure.persistence;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import com.almoxarifado.alerta.application.AlertaRepository;
import com.almoxarifado.alerta.domain.Alerta;
import com.almoxarifado.shared.infrastructure.persistence.FiltroEscopoSql;
@Repository
public class AlertaJdbc implements AlertaRepository {
 private final JdbcTemplate jdbc;
 public AlertaJdbc(JdbcTemplate jdbc) {this.jdbc=jdbc;}
 private static final String ATIVOS="""
  WITH contexto AS (SELECT ?::uuid AS org,?::uuid[] AS escopo),
  saldos AS (
   SELECT s.*,l.almoxarifado_id FROM alerta.saldos_projecao s
   LEFT JOIN alerta.locais_projecao l ON l.organizacao_id=s.organizacao_id AND l.id=s.local_id
   WHERE s.organizacao_id=(SELECT org FROM contexto)
  ), limites AS (
   SELECT p.item_id,p.almoxarifado_id,p.minimo,p.maximo,
    coalesce(sum(CASE WHEN s.validade IS NULL OR s.validade>=CURRENT_DATE THEN s.quantidade-s.reservado ELSE 0 END),0) AS disponivel,
    coalesce(sum(s.quantidade),0) AS fisico,
    md5(coalesce(string_agg(s.id::text||':'||s.revisao,',' ORDER BY s.id),'')||p.atualizado_em::text) AS revisao
   FROM alerta.parametros_reposicao p
   JOIN alerta.itens_projecao i ON i.organizacao_id=p.organizacao_id AND i.id=p.item_id AND i.ativo
   JOIN alerta.almoxarifados_projecao a ON a.organizacao_id=p.organizacao_id AND a.id=p.almoxarifado_id AND a.ativo
   LEFT JOIN saldos s ON s.organizacao_id=p.organizacao_id AND s.item_id=p.item_id AND s.almoxarifado_id=p.almoxarifado_id
   WHERE p.organizacao_id=(SELECT org FROM contexto)
    AND ((SELECT escopo FROM contexto) IS NULL OR p.almoxarifado_id=ANY((SELECT escopo FROM contexto)::uuid[]))
   GROUP BY p.item_id,p.almoxarifado_id,p.minimo,p.maximo,p.atualizado_em
  ), ativos AS (
   SELECT 'MINIMO:'||i.id AS chave,'ESTOQUE_MINIMO' AS tipo,i.id AS item_id,NULL::uuid AS posicao_id,
    coalesce(sum(CASE WHEN s.validade IS NULL OR s.validade>=CURRENT_DATE THEN s.quantidade-s.reservado ELSE 0 END),0) AS quantidade,
    i.minimo AS limite,NULL::date AS validade,
    md5(coalesce(string_agg(s.id::text||':'||s.revisao,',' ORDER BY s.id),'')||i.atualizado_em::text) AS revisao,
    NULL::uuid AS almoxarifado_id
   FROM alerta.itens_projecao i LEFT JOIN saldos s ON s.organizacao_id=i.organizacao_id AND s.item_id=i.id
   WHERE i.organizacao_id=(SELECT org FROM contexto) AND i.ativo AND (SELECT escopo FROM contexto) IS NULL
    AND NOT EXISTS(SELECT 1 FROM alerta.parametros_reposicao p WHERE p.organizacao_id=i.organizacao_id AND p.item_id=i.id)
   GROUP BY i.id,i.minimo,i.atualizado_em
   HAVING coalesce(sum(CASE WHEN s.validade IS NULL OR s.validade>=CURRENT_DATE THEN s.quantidade-s.reservado ELSE 0 END),0)<i.minimo
   UNION ALL
   SELECT 'MINIMO_ALMOX:'||item_id||':'||almoxarifado_id,'ESTOQUE_MINIMO',item_id,NULL::uuid,disponivel,minimo,NULL::date,revisao,almoxarifado_id
    FROM limites WHERE disponivel<minimo
   UNION ALL
   SELECT 'MAXIMO_ALMOX:'||item_id||':'||almoxarifado_id,'ESTOQUE_MAXIMO',item_id,NULL::uuid,fisico,maximo,NULL::date,revisao,almoxarifado_id
    FROM limites WHERE fisico>maximo
   UNION ALL
   SELECT (CASE WHEN s.validade<CURRENT_DATE THEN 'VENCIDO:' ELSE 'VALIDADE:' END)||s.id,
    CASE WHEN s.validade<CURRENT_DATE THEN 'LOTE_VENCIDO' ELSE 'VALIDADE_PROXIMA' END,
    s.item_id,s.id,s.quantidade,NULL::numeric,s.validade,s.revisao::text,s.almoxarifado_id
   FROM saldos s JOIN alerta.itens_projecao i ON i.organizacao_id=s.organizacao_id AND i.id=s.item_id
   WHERE i.ativo AND s.quantidade>0 AND s.validade<=CURRENT_DATE+coalesce((SELECT c.dias FROM alerta.configuracoes c WHERE c.organizacao_id=s.organizacao_id),30)
    AND ((SELECT escopo FROM contexto) IS NULL OR s.almoxarifado_id=ANY((SELECT escopo FROM contexto)::uuid[]))
  ), visiveis AS (
   SELECT * FROM ativos a WHERE NOT EXISTS(SELECT 1 FROM alerta.dispensas d WHERE d.organizacao_id=(SELECT org FROM contexto) AND d.chave=a.chave AND d.revisao=a.revisao)
  )
  """;
 private static final RowMapper<Alerta> MAP=(rs,n)->new Alerta(rs.getString("chave"),rs.getString("tipo"),rs.getObject("item_id",UUID.class),rs.getObject("posicao_id",UUID.class),rs.getBigDecimal("quantidade"),rs.getBigDecimal("limite"),rs.getObject("validade",java.time.LocalDate.class),rs.getString("revisao"),rs.getObject("almoxarifado_id",UUID.class));
 public List<Alerta> listar(UUID org,int limite,int deslocamento,Set<UUID> escopo) {
  return jdbc.query(ATIVOS+" SELECT * FROM visiveis ORDER BY tipo,chave LIMIT ? OFFSET ?",MAP,org,FiltroEscopoSql.array(escopo),limite,deslocamento);
 }
 public Optional<Alerta> buscar(UUID org,String chave,String revisao,Set<UUID> escopo) {
  return jdbc.query(ATIVOS+" SELECT * FROM visiveis WHERE chave=? AND revisao=?",MAP,org,FiltroEscopoSql.array(escopo),chave,revisao).stream().findFirst();
 }
 public void dispensar(UUID org,String chave,String revisao,String usuario) {
  jdbc.update("INSERT INTO alerta.dispensas(organizacao_id,chave,revisao,usuario_id) VALUES(?,?,?,?) ON CONFLICT DO NOTHING",org,chave,revisao,usuario);
 }
 public Map<String,Object> dashboard(UUID org,Set<UUID> escopo) {
  var valores=jdbc.queryForMap(ATIVOS+"""
   SELECT count(*) AS alertas_ativos,count(DISTINCT item_id) FILTER(WHERE tipo='ESTOQUE_MINIMO') AS itens_abaixo_minimo,
    count(DISTINCT item_id) FILTER(WHERE tipo='ESTOQUE_MAXIMO') AS itens_acima_maximo,
    count(*) FILTER(WHERE tipo='LOTE_VENCIDO') AS lotes_vencidos,count(*) FILTER(WHERE tipo='VALIDADE_PROXIMA') AS lotes_a_vencer FROM visiveis
   """,org,FiltroEscopoSql.array(escopo));
  valores.put("itens_ativos",jdbc.queryForObject("SELECT count(*) FROM alerta.itens_projecao WHERE organizacao_id=? AND ativo",Long.class,org));return valores;
 }
}
