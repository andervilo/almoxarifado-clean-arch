package com.almoxarifado.alerta.infrastructure.persistence;
import java.util.*;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import com.almoxarifado.alerta.application.AlertaRepository;
import com.almoxarifado.alerta.domain.Alerta;
@Repository
public class AlertaJdbc implements AlertaRepository {
 private final JdbcTemplate jdbc;
 public AlertaJdbc(JdbcTemplate jdbc) { this.jdbc=jdbc; }
 private static final String ATIVOS="""
  WITH ativos AS (
   SELECT 'MINIMO:'||i.id AS chave,'ESTOQUE_MINIMO' AS tipo,i.id AS item_id,NULL::uuid AS posicao_id,
    coalesce(sum(CASE WHEN s.validade IS NULL OR s.validade>=CURRENT_DATE THEN s.quantidade-s.reservado ELSE 0 END),0) AS quantidade,
    i.minimo AS limite,NULL::date AS validade,
    md5(coalesce(string_agg(s.id::text||':'||s.revisao,',' ORDER BY s.id),'')||i.atualizado_em::text) AS revisao
   FROM alerta.itens_projecao i LEFT JOIN alerta.saldos_projecao s ON s.organizacao_id=i.organizacao_id AND s.item_id=i.id
   WHERE i.organizacao_id=? AND i.ativo GROUP BY i.id,i.minimo,i.atualizado_em
   HAVING coalesce(sum(CASE WHEN s.validade IS NULL OR s.validade>=CURRENT_DATE THEN s.quantidade-s.reservado ELSE 0 END),0)<i.minimo
   UNION ALL
   SELECT (CASE WHEN s.validade<CURRENT_DATE THEN 'VENCIDO:' ELSE 'VALIDADE:' END)||s.id,
    CASE WHEN s.validade<CURRENT_DATE THEN 'LOTE_VENCIDO' ELSE 'VALIDADE_PROXIMA' END,
    s.item_id,s.id,s.quantidade,NULL::numeric,s.validade,s.revisao::text
   FROM alerta.saldos_projecao s JOIN alerta.itens_projecao i ON i.organizacao_id=s.organizacao_id AND i.id=s.item_id
   WHERE s.organizacao_id=? AND i.ativo AND s.quantidade>0 AND s.validade<=CURRENT_DATE+coalesce((SELECT c.dias FROM alerta.configuracoes c WHERE c.organizacao_id=s.organizacao_id),30)
  ), visiveis AS (
   SELECT * FROM ativos a WHERE NOT EXISTS (SELECT 1 FROM alerta.dispensas d WHERE d.organizacao_id=? AND d.chave=a.chave AND d.revisao=a.revisao)
  )
  """;
 private static final RowMapper<Alerta> MAP=(rs,n)->new Alerta(rs.getString("chave"),rs.getString("tipo"),rs.getObject("item_id",UUID.class),rs.getObject("posicao_id",UUID.class),rs.getBigDecimal("quantidade"),rs.getBigDecimal("limite"),rs.getObject("validade",java.time.LocalDate.class),rs.getString("revisao"));
 public List<Alerta> listar(UUID org,int limite,int deslocamento) { return jdbc.query(ATIVOS+" SELECT * FROM visiveis ORDER BY tipo,chave LIMIT ? OFFSET ?",MAP,org,org,org,limite,deslocamento); }
 public Optional<Alerta> buscar(UUID org,String chave,String revisao) { return jdbc.query(ATIVOS+" SELECT * FROM visiveis WHERE chave=? AND revisao=?",MAP,org,org,org,chave,revisao).stream().findFirst(); }
 public void dispensar(UUID org,String chave,String revisao,String usuario) {
  jdbc.update("INSERT INTO alerta.dispensas(organizacao_id,chave,revisao,usuario_id) VALUES(?,?,?,?) ON CONFLICT DO NOTHING",org,chave,revisao,usuario);
 }
 public Map<String,Object> dashboard(UUID org) {
  var valores=jdbc.queryForMap(ATIVOS+" SELECT count(*) AS alertas_ativos,count(*) FILTER(WHERE tipo='ESTOQUE_MINIMO') AS itens_abaixo_minimo,count(*) FILTER(WHERE tipo='LOTE_VENCIDO') AS lotes_vencidos,count(*) FILTER(WHERE tipo='VALIDADE_PROXIMA') AS lotes_a_vencer FROM visiveis",org,org,org);
  valores.put("itens_ativos",jdbc.queryForObject("SELECT count(*) FROM alerta.itens_projecao WHERE organizacao_id=? AND ativo",Long.class,org));return valores;
 }
}
