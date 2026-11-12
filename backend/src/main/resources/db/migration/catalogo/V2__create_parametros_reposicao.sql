CREATE TABLE almoxarifados_projecao(organizacao_id UUID NOT NULL,id UUID NOT NULL,ativo BOOLEAN NOT NULL,
 atualizado_em TIMESTAMPTZ NOT NULL,PRIMARY KEY(organizacao_id,id));
CREATE TABLE parametros_reposicao(organizacao_id UUID NOT NULL,id UUID NOT NULL,item_id UUID NOT NULL,almoxarifado_id UUID NOT NULL,
 minimo NUMERIC(19,4) NOT NULL CHECK(minimo>=0),maximo NUMERIC(19,4) NOT NULL CHECK(maximo>=minimo),
 PRIMARY KEY(organizacao_id,id),UNIQUE(organizacao_id,item_id,almoxarifado_id),
 FOREIGN KEY(organizacao_id,item_id) REFERENCES itens(organizacao_id,id),
 FOREIGN KEY(organizacao_id,almoxarifado_id) REFERENCES almoxarifados_projecao(organizacao_id,id));
