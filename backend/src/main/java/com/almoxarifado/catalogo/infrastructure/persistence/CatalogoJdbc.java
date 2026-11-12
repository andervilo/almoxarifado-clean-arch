package com.almoxarifado.catalogo.infrastructure.persistence;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import com.almoxarifado.catalogo.application.CatalogoRepository;
import com.almoxarifado.catalogo.domain.*;
@Repository
public class CatalogoJdbc implements CatalogoRepository {
 private final JdbcTemplate jdbc;
 public CatalogoJdbc(JdbcTemplate jdbc) { this.jdbc=jdbc; }
 public boolean organizacaoAtiva(UUID org) {
  return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM catalogo.organizacoes_projecao WHERE id=? AND ativa)",Boolean.class,org));
 }
 private static final RowMapper<Categoria> MAP_CATEGORIA=(rs,n)->new Categoria(rs.getObject("id",UUID.class),rs.getObject("organizacao_id",UUID.class),rs.getString("codigo"),rs.getString("nome"),rs.getBoolean("ativo"));
 public Optional<Categoria> buscarCategoria(UUID org,UUID id) {
  return jdbc.query("SELECT * FROM catalogo.categorias WHERE organizacao_id=? AND id=?",MAP_CATEGORIA,org,id).stream().findFirst();
 }
 public List<Categoria> listarCategoria(UUID org,int limite,int deslocamento) {
  return jdbc.query("SELECT * FROM catalogo.categorias WHERE organizacao_id=? ORDER BY codigo,id LIMIT ? OFFSET ?",MAP_CATEGORIA,org,limite,deslocamento);
 }
 public void salvar(Categoria v) {
  jdbc.update("INSERT INTO catalogo.categorias (id,organizacao_id,codigo,nome,ativo) VALUES (?,?,?,?,?) ON CONFLICT (organizacao_id,id) DO UPDATE SET codigo=excluded.codigo,nome=excluded.nome,ativo=excluded.ativo",v.id(),v.organizacaoId(),v.codigo(),v.nome(),v.ativo());
 }
 private static final RowMapper<UnidadeMedida> MAP_UNIDADEMEDIDA=(rs,n)->new UnidadeMedida(rs.getObject("id",UUID.class),rs.getObject("organizacao_id",UUID.class),rs.getString("codigo"),rs.getString("nome"),rs.getBoolean("ativo"));
 public Optional<UnidadeMedida> buscarUnidadeMedida(UUID org,UUID id) {
  return jdbc.query("SELECT * FROM catalogo.unidades_medida WHERE organizacao_id=? AND id=?",MAP_UNIDADEMEDIDA,org,id).stream().findFirst();
 }
 public List<UnidadeMedida> listarUnidadeMedida(UUID org,int limite,int deslocamento) {
  return jdbc.query("SELECT * FROM catalogo.unidades_medida WHERE organizacao_id=? ORDER BY codigo,id LIMIT ? OFFSET ?",MAP_UNIDADEMEDIDA,org,limite,deslocamento);
 }
 public void salvar(UnidadeMedida v) {
  jdbc.update("INSERT INTO catalogo.unidades_medida (id,organizacao_id,codigo,nome,ativo) VALUES (?,?,?,?,?) ON CONFLICT (organizacao_id,id) DO UPDATE SET codigo=excluded.codigo,nome=excluded.nome,ativo=excluded.ativo",v.id(),v.organizacaoId(),v.codigo(),v.nome(),v.ativo());
 }
 private static final RowMapper<Fornecedor> MAP_FORNECEDOR=(rs,n)->new Fornecedor(rs.getObject("id",UUID.class),rs.getObject("organizacao_id",UUID.class),rs.getString("codigo"),rs.getString("nome"),rs.getBoolean("ativo"));
 public Optional<Fornecedor> buscarFornecedor(UUID org,UUID id) {
  return jdbc.query("SELECT * FROM catalogo.fornecedores WHERE organizacao_id=? AND id=?",MAP_FORNECEDOR,org,id).stream().findFirst();
 }
 public List<Fornecedor> listarFornecedor(UUID org,int limite,int deslocamento) {
  return jdbc.query("SELECT * FROM catalogo.fornecedores WHERE organizacao_id=? ORDER BY codigo,id LIMIT ? OFFSET ?",MAP_FORNECEDOR,org,limite,deslocamento);
 }
 public void salvar(Fornecedor v) {
  jdbc.update("INSERT INTO catalogo.fornecedores (id,organizacao_id,codigo,nome,ativo) VALUES (?,?,?,?,?) ON CONFLICT (organizacao_id,id) DO UPDATE SET codigo=excluded.codigo,nome=excluded.nome,ativo=excluded.ativo",v.id(),v.organizacaoId(),v.codigo(),v.nome(),v.ativo());
 }
 private static final RowMapper<Item> MAP_ITEM=(rs,n)->new Item(rs.getObject("id",UUID.class),rs.getObject("organizacao_id",UUID.class),rs.getString("codigo"),rs.getString("descricao"),rs.getObject("categoria_id",UUID.class),rs.getObject("unidade_medida_id",UUID.class),TipoRastreabilidade.valueOf(rs.getString("rastreabilidade")),rs.getBigDecimal("estoque_minimo"),rs.getBigDecimal("estoque_maximo"),rs.getBoolean("ativo"));
 public Optional<Item> buscarItem(UUID org,UUID id) {
  return jdbc.query("SELECT * FROM catalogo.itens WHERE organizacao_id=? AND id=?",MAP_ITEM,org,id).stream().findFirst();
 }
 public List<Item> listarItem(UUID org,int limite,int deslocamento) {
  return jdbc.query("SELECT * FROM catalogo.itens WHERE organizacao_id=? ORDER BY codigo,id LIMIT ? OFFSET ?",MAP_ITEM,org,limite,deslocamento);
 }
 public void salvar(Item v) {
  jdbc.update("INSERT INTO catalogo.itens (id,organizacao_id,codigo,descricao,categoria_id,unidade_medida_id,rastreabilidade,estoque_minimo,estoque_maximo,ativo) VALUES (?,?,?,?,?,?,?,?,?,?) ON CONFLICT (organizacao_id,id) DO UPDATE SET codigo=excluded.codigo,descricao=excluded.descricao,categoria_id=excluded.categoria_id,unidade_medida_id=excluded.unidade_medida_id,rastreabilidade=excluded.rastreabilidade,estoque_minimo=excluded.estoque_minimo,estoque_maximo=excluded.estoque_maximo,ativo=excluded.ativo",v.id(),v.organizacaoId(),v.codigo(),v.descricao(),v.categoriaId(),v.unidadeMedidaId(),v.rastreabilidade().name(),v.estoqueMinimo(),v.estoqueMaximo(),v.ativo());
 }
}
