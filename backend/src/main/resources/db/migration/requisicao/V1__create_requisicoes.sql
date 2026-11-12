CREATE TABLE requisicoes(organizacao_id UUID NOT NULL,id UUID NOT NULL,solicitante_id VARCHAR(255) NOT NULL,
 local_id UUID NOT NULL,status VARCHAR(30) NOT NULL,comando_id UUID,erro VARCHAR(255),criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(organizacao_id,id));
CREATE TABLE linhas(organizacao_id UUID NOT NULL,requisicao_id UUID NOT NULL,item_id UUID NOT NULL,
 quantidade NUMERIC(19,4) NOT NULL CHECK(quantidade>0),PRIMARY KEY(organizacao_id,requisicao_id,item_id),
 FOREIGN KEY(organizacao_id,requisicao_id) REFERENCES requisicoes(organizacao_id,id));
