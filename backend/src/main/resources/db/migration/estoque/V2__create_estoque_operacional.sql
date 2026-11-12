CREATE TABLE organizacoes_projecao(id UUID PRIMARY KEY,ativa BOOLEAN NOT NULL,atualizado_em TIMESTAMPTZ NOT NULL);
CREATE TABLE itens_projecao(organizacao_id UUID NOT NULL,id UUID NOT NULL,ativo BOOLEAN NOT NULL,rastreabilidade VARCHAR(30) NOT NULL,
 estoque_minimo NUMERIC(19,4) NOT NULL,atualizado_em TIMESTAMPTZ NOT NULL,PRIMARY KEY(organizacao_id,id));
CREATE TABLE almoxarifados_projecao(organizacao_id UUID NOT NULL,id UUID NOT NULL,ativo BOOLEAN NOT NULL,
 atualizado_em TIMESTAMPTZ NOT NULL,PRIMARY KEY(organizacao_id,id));
CREATE TABLE locais_projecao(organizacao_id UUID NOT NULL,id UUID NOT NULL,almoxarifado_id UUID NOT NULL,ativo BOOLEAN NOT NULL,
 atualizado_em TIMESTAMPTZ NOT NULL,PRIMARY KEY(organizacao_id,id));
CREATE TABLE posicoes(organizacao_id UUID NOT NULL,id UUID NOT NULL,item_id UUID NOT NULL,local_id UUID NOT NULL,
 lote VARCHAR(120) NOT NULL DEFAULT '',serie VARCHAR(120) NOT NULL DEFAULT '',validade DATE,
 recebido_em TIMESTAMPTZ NOT NULL,quantidade NUMERIC(19,4) NOT NULL CHECK(quantidade>=0),
 reservado NUMERIC(19,4) NOT NULL DEFAULT 0 CHECK(reservado>=0 AND reservado<=quantidade),
 custo NUMERIC(19,6) NOT NULL CHECK(custo>=0),revisao BIGINT NOT NULL DEFAULT 1,PRIMARY KEY(organizacao_id,id),
 UNIQUE(organizacao_id,item_id,local_id,lote,serie));
CREATE UNIQUE INDEX uk_serie_item ON posicoes(organizacao_id,item_id,serie) WHERE serie<>'' AND quantidade>0;
CREATE TABLE operacoes(organizacao_id UUID NOT NULL,id UUID NOT NULL,item_id UUID NOT NULL,tipo VARCHAR(30) NOT NULL,
 assinatura TEXT NOT NULL,usuario_id VARCHAR(255) NOT NULL,motivo VARCHAR(500) NOT NULL,
 criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(organizacao_id,id));
CREATE TABLE linhas_operacao(id BIGSERIAL PRIMARY KEY,organizacao_id UUID NOT NULL,operacao_id UUID NOT NULL,posicao_id UUID NOT NULL,
 quantidade NUMERIC(19,4) NOT NULL,custo NUMERIC(19,6) NOT NULL,
 FOREIGN KEY(organizacao_id,operacao_id) REFERENCES operacoes(organizacao_id,id),
 FOREIGN KEY(organizacao_id,posicao_id) REFERENCES posicoes(organizacao_id,id));
CREATE TABLE reservas(organizacao_id UUID NOT NULL,requisicao_id UUID NOT NULL,posicao_id UUID NOT NULL,
 quantidade NUMERIC(19,4) NOT NULL CHECK(quantidade>0),status VARCHAR(20) NOT NULL,
 PRIMARY KEY(organizacao_id,requisicao_id,posicao_id),
 FOREIGN KEY(organizacao_id,posicao_id) REFERENCES posicoes(organizacao_id,id));
CREATE FUNCTION impedir_alteracao_movimento() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Movimentacoes sao imutaveis; utilize estorno ou ajuste'; END; $$;
CREATE TRIGGER operacoes_imutaveis BEFORE UPDATE OR DELETE OR TRUNCATE ON operacoes FOR EACH STATEMENT EXECUTE FUNCTION impedir_alteracao_movimento();
CREATE TRIGGER linhas_imutaveis BEFORE UPDATE OR DELETE OR TRUNCATE ON linhas_operacao FOR EACH STATEMENT EXECUTE FUNCTION impedir_alteracao_movimento();
CREATE INDEX idx_posicoes_selecao ON posicoes(organizacao_id,item_id,local_id,validade,recebido_em);
CREATE TABLE custos(organizacao_id UUID NOT NULL,item_id UUID NOT NULL,almoxarifado_id UUID NOT NULL,
 quantidade NUMERIC(19,4) NOT NULL CHECK(quantidade>=0),custo NUMERIC(19,6) NOT NULL CHECK(custo>=0),
 PRIMARY KEY(organizacao_id,item_id,almoxarifado_id));
CREATE TABLE estornos(organizacao_id UUID NOT NULL,original_id UUID NOT NULL,estorno_id UUID NOT NULL,
 PRIMARY KEY(organizacao_id,original_id),FOREIGN KEY(organizacao_id,original_id) REFERENCES operacoes(organizacao_id,id),
 FOREIGN KEY(organizacao_id,estorno_id) REFERENCES operacoes(organizacao_id,id));
CREATE TABLE processamentos_integracao(organizacao_id UUID NOT NULL,evento_id UUID NOT NULL,PRIMARY KEY(organizacao_id,evento_id));
