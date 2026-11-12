CREATE TABLE itens_projecao(organizacao_id UUID NOT NULL,id UUID NOT NULL,ativo BOOLEAN NOT NULL,minimo NUMERIC(19,4) NOT NULL,
 atualizado_em TIMESTAMPTZ NOT NULL,PRIMARY KEY(organizacao_id,id));
CREATE TABLE configuracoes(organizacao_id UUID PRIMARY KEY,dias INTEGER NOT NULL,atualizado_em TIMESTAMPTZ NOT NULL);
CREATE TABLE saldos_projecao(organizacao_id UUID NOT NULL,id UUID NOT NULL,item_id UUID NOT NULL,local_id UUID NOT NULL,
 quantidade NUMERIC(19,4) NOT NULL,reservado NUMERIC(19,4) NOT NULL,validade DATE,revisao BIGINT NOT NULL,PRIMARY KEY(organizacao_id,id));
CREATE TABLE dispensas(organizacao_id UUID NOT NULL,chave VARCHAR(150) NOT NULL,revisao VARCHAR(100) NOT NULL,
 usuario_id VARCHAR(255) NOT NULL,dispensado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(organizacao_id,chave,revisao));
