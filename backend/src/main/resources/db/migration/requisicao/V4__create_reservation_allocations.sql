CREATE TABLE alocacoes_revisoes(organizacao_id UUID NOT NULL,requisicao_id UUID NOT NULL,item_id UUID NOT NULL,revisao BIGINT NOT NULL,
 PRIMARY KEY(organizacao_id,requisicao_id,item_id));
CREATE TABLE alocacoes(organizacao_id UUID NOT NULL,requisicao_id UUID NOT NULL,item_id UUID NOT NULL,posicao_id UUID NOT NULL,
 local_id UUID NOT NULL,lote VARCHAR(120) NOT NULL,serie VARCHAR(120) NOT NULL,validade DATE,quantidade NUMERIC(19,4) NOT NULL CHECK(quantidade>0),
 PRIMARY KEY(organizacao_id,requisicao_id,item_id,posicao_id));
CREATE TABLE chaves_realocacao(organizacao_id UUID NOT NULL,chave UUID NOT NULL,requisicao_id UUID NOT NULL,assinatura TEXT NOT NULL,comando_id UUID NOT NULL,
 PRIMARY KEY(organizacao_id,chave),FOREIGN KEY(organizacao_id,requisicao_id) REFERENCES requisicoes(organizacao_id,id));
