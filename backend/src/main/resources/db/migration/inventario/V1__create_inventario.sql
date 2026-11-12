CREATE TABLE saldos_projecao(organizacao_id UUID NOT NULL,id UUID NOT NULL,item_id UUID NOT NULL,local_id UUID NOT NULL,
 lote VARCHAR(120) NOT NULL,serie VARCHAR(120) NOT NULL,validade DATE,quantidade NUMERIC(19,4) NOT NULL,
 custo NUMERIC(19,6) NOT NULL,revisao BIGINT NOT NULL,PRIMARY KEY(organizacao_id,id));
CREATE TABLE inventarios(organizacao_id UUID NOT NULL,id UUID NOT NULL,local_id UUID NOT NULL,status VARCHAR(30) NOT NULL,
 comando_id UUID,erro VARCHAR(255),criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(organizacao_id,id));
CREATE UNIQUE INDEX uk_inventario_ativo_local ON inventarios(organizacao_id,local_id) WHERE status IN ('CONTAGEM','FINALIZANDO');
CREATE TABLE contagens(organizacao_id UUID NOT NULL,inventario_id UUID NOT NULL,posicao_id UUID NOT NULL,
 item_id UUID NOT NULL,local_id UUID NOT NULL,lote VARCHAR(120) NOT NULL,serie VARCHAR(120) NOT NULL,validade DATE,
 sistema NUMERIC(19,4) NOT NULL,contada NUMERIC(19,4) CHECK(contada>=0),custo NUMERIC(19,6) NOT NULL,revisao BIGINT NOT NULL,
 PRIMARY KEY(organizacao_id,inventario_id,posicao_id),FOREIGN KEY(organizacao_id,inventario_id) REFERENCES inventarios(organizacao_id,id));
