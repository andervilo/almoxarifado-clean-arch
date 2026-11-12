ALTER TABLE linhas ADD COLUMN atendida NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE linhas ADD CONSTRAINT ck_atendimento_linha CHECK(atendida>=0 AND atendida<=quantidade);
ALTER TABLE requisicoes ADD COLUMN estado_anterior VARCHAR(30);
-- Preserva o significado dos pedidos entregues antes da adicao do contador.
UPDATE linhas l SET atendida=l.quantidade FROM requisicoes r
WHERE r.organizacao_id=l.organizacao_id AND r.id=l.requisicao_id AND r.status IN ('ENTREGUE','FINALIZADA');
CREATE TABLE entregas_pendentes(organizacao_id UUID NOT NULL,requisicao_id UUID NOT NULL,comando_id UUID NOT NULL,item_id UUID NOT NULL,
 quantidade NUMERIC(19,4) NOT NULL CHECK(quantidade>0),confirmada BOOLEAN NOT NULL DEFAULT FALSE,
 criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(organizacao_id,comando_id,item_id),
 FOREIGN KEY(organizacao_id,requisicao_id,item_id) REFERENCES linhas(organizacao_id,requisicao_id,item_id));
CREATE TABLE chaves_entrega(organizacao_id UUID NOT NULL,chave UUID NOT NULL,requisicao_id UUID NOT NULL,
 assinatura TEXT NOT NULL,comando_id UUID NOT NULL,PRIMARY KEY(organizacao_id,chave),
 FOREIGN KEY(organizacao_id,requisicao_id) REFERENCES requisicoes(organizacao_id,id));
