CREATE TABLE setores_projecao(organizacao_id UUID NOT NULL,codigo VARCHAR(120) NOT NULL,nome VARCHAR(255) NOT NULL,
 centro_custo VARCHAR(120) NOT NULL,atualizado_em TIMESTAMPTZ NOT NULL,PRIMARY KEY(organizacao_id,codigo));
ALTER TABLE requisicoes ADD COLUMN setor_codigo VARCHAR(120);
ALTER TABLE requisicoes ADD COLUMN centro_custo VARCHAR(120) NOT NULL DEFAULT '';
CREATE TABLE consumo(organizacao_id UUID NOT NULL,operacao_id UUID NOT NULL,requisicao_id UUID NOT NULL,item_id UUID NOT NULL,
 quantidade NUMERIC(19,4) NOT NULL,custo_total NUMERIC(40,6) NOT NULL,efetivado_em TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(organizacao_id,operacao_id));
CREATE INDEX idx_consumo_periodo ON consumo(organizacao_id,efetivado_em);
