CREATE TABLE entregas_requisicao(organizacao_id UUID NOT NULL,operacao_id UUID NOT NULL,requisicao_id UUID NOT NULL,item_id UUID NOT NULL,
 quantidade NUMERIC(19,4) NOT NULL CHECK(quantidade<>0),custo_total NUMERIC(40,6) NOT NULL,
 efetivado_em TIMESTAMPTZ NOT NULL,PRIMARY KEY(organizacao_id,operacao_id),
 FOREIGN KEY(organizacao_id,operacao_id) REFERENCES operacoes(organizacao_id,id));
-- O formato historico de ComandoEstoque termina com referenciaId=<UUID>].
-- Apenas lemos o historico imutavel; os novos registros recebem o vinculo explicitamente.
WITH RECURSIVE consumo(organizacao_id,id,requisicao_id,item_id,quantidade,custo_total,criado_em) AS (
 SELECT o.organizacao_id,o.id,substring(o.assinatura from 'referenciaId=([0-9a-fA-F-]{36})\]$')::uuid,
  o.item_id,sum(-l.quantidade),sum(-l.quantidade*l.custo),o.criado_em
 FROM operacoes o JOIN linhas_operacao l ON l.organizacao_id=o.organizacao_id AND l.operacao_id=o.id
 WHERE o.tipo='ENTREGA' GROUP BY o.organizacao_id,o.id,o.assinatura,o.item_id,o.criado_em
 UNION ALL
 SELECT c.organizacao_id,e.estorno_id,c.requisicao_id,c.item_id,-c.quantidade,-c.custo_total,o.criado_em
 FROM consumo c JOIN estornos e ON e.organizacao_id=c.organizacao_id AND e.original_id=c.id
 JOIN operacoes o ON o.organizacao_id=e.organizacao_id AND o.id=e.estorno_id
)
INSERT INTO entregas_requisicao(organizacao_id,operacao_id,requisicao_id,item_id,quantidade,custo_total,efetivado_em)
 SELECT * FROM consumo;
CREATE TRIGGER entregas_requisicao_imutaveis BEFORE UPDATE OR DELETE OR TRUNCATE ON entregas_requisicao
FOR EACH STATEMENT EXECUTE FUNCTION impedir_alteracao_movimento();
