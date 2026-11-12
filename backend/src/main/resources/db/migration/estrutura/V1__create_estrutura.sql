CREATE TABLE organizacoes_projecao (id UUID PRIMARY KEY, ativa BOOLEAN NOT NULL, atualizado_em TIMESTAMPTZ NOT NULL);
CREATE TABLE almoxarifados (organizacao_id UUID NOT NULL REFERENCES organizacoes_projecao(id), id UUID NOT NULL,
 codigo VARCHAR(120) NOT NULL, nome VARCHAR(255) NOT NULL, ativo BOOLEAN NOT NULL,
 PRIMARY KEY(organizacao_id,id), UNIQUE(organizacao_id,codigo));
CREATE TABLE locais (organizacao_id UUID NOT NULL, id UUID NOT NULL, almoxarifado_id UUID NOT NULL,
 codigo VARCHAR(120) NOT NULL, descricao VARCHAR(255) NOT NULL, ativo BOOLEAN NOT NULL,
 PRIMARY KEY(organizacao_id,id), UNIQUE(organizacao_id,almoxarifado_id,codigo),
 FOREIGN KEY(organizacao_id,almoxarifado_id) REFERENCES almoxarifados(organizacao_id,id));
