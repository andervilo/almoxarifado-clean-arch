CREATE TABLE locais_projecao(organizacao_id UUID NOT NULL,id UUID NOT NULL,almoxarifado_id UUID NOT NULL,
 atualizado_em TIMESTAMPTZ NOT NULL,PRIMARY KEY(organizacao_id,id));
