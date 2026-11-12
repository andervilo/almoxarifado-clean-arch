package com.almoxarifado.requisicao.infrastructure.messaging;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import com.almoxarifado.requisicao.events.AlocacoesRequisicaoAtualizadas;
@Component
public class AlocacoesProjetor {
 private final JdbcTemplate jdbc;
 public AlocacoesProjetor(JdbcTemplate jdbc) {this.jdbc=jdbc;}
 @ApplicationModuleListener public void receber(AlocacoesRequisicaoAtualizadas e) {
  int alteradas=jdbc.update("""
   INSERT INTO requisicao.alocacoes_revisoes(organizacao_id,requisicao_id,item_id,revisao) VALUES(?,?,?,?)
   ON CONFLICT(organizacao_id,requisicao_id,item_id) DO UPDATE SET revisao=excluded.revisao
   WHERE requisicao.alocacoes_revisoes.revisao<=excluded.revisao
   """,e.organizacaoId(),e.requisicaoId(),e.itemId(),e.revisao());
  if(alteradas==0) return;
  jdbc.update("DELETE FROM requisicao.alocacoes WHERE organizacao_id=? AND requisicao_id=? AND item_id=?",e.organizacaoId(),e.requisicaoId(),e.itemId());
  for(var a:e.alocacoes()) jdbc.update("INSERT INTO requisicao.alocacoes(organizacao_id,requisicao_id,item_id,posicao_id,local_id,lote,serie,validade,quantidade) VALUES(?,?,?,?,?,?,?,?,?)",e.organizacaoId(),e.requisicaoId(),e.itemId(),a.posicaoId(),a.localId(),a.lote(),a.serie(),a.validade(),a.quantidade());
 }
}
