package com.almoxarifado.inventario.infrastructure.messaging;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import com.almoxarifado.estoque.events.SaldoAlterado;
import com.almoxarifado.shared.events.AjusteInventarioConcluido;
import com.almoxarifado.inventario.application.InventarioRepository;
import com.almoxarifado.shared.application.Auditoria;
@Component
public class InventarioProjetor {
 private final JdbcTemplate jdbc;private final InventarioRepository repository;private final Auditoria auditoria;
 public InventarioProjetor(JdbcTemplate jdbc,InventarioRepository repository,Auditoria auditoria) { this.jdbc=jdbc;this.repository=repository;this.auditoria=auditoria; }
 @ApplicationModuleListener public void saldo(SaldoAlterado e) {
  jdbc.update("""
   INSERT INTO inventario.saldos_projecao(organizacao_id,id,item_id,local_id,lote,serie,validade,quantidade,custo,revisao) VALUES(?,?,?,?,?,?,?,?,?,?)
   ON CONFLICT(organizacao_id,id) DO UPDATE SET quantidade=excluded.quantidade,custo=excluded.custo,revisao=excluded.revisao
   WHERE inventario.saldos_projecao.revisao<excluded.revisao
   """,e.organizacaoId(),e.posicaoId(),e.itemId(),e.localId(),e.lote(),e.serie(),e.validade(),e.quantidade(),e.custo(),e.revisao());
 }
 @ApplicationModuleListener public void resultado(AjusteInventarioConcluido e) {
  var inv=repository.buscar(e.organizacaoId(),e.inventarioId(),true).orElseThrow();
  if(!e.comandoId().equals(inv.comandoId()) || !inv.status().equals("FINALIZANDO")) return;
  String estado=e.sucesso()?"CONCLUIDO":"CONFLITO";repository.estado(e.organizacaoId(),e.inventarioId(),estado,null,e.erro());
  auditoria.registrar(e.organizacaoId(),"sistema","INVENTARIO_"+estado,e.inventarioId().toString());
 }
}
