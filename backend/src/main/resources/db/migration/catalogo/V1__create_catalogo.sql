CREATE TABLE organizacoes_projecao (id UUID PRIMARY KEY, ativa BOOLEAN NOT NULL, atualizado_em TIMESTAMPTZ NOT NULL);
CREATE TABLE categorias (id UUID NOT NULL, organizacao_id UUID NOT NULL REFERENCES organizacoes_projecao(id),
 codigo VARCHAR(120) NOT NULL, nome VARCHAR(255) NOT NULL, ativo BOOLEAN NOT NULL,
 PRIMARY KEY (organizacao_id,id), UNIQUE(organizacao_id,codigo));
CREATE TABLE unidades_medida (id UUID NOT NULL, organizacao_id UUID NOT NULL REFERENCES organizacoes_projecao(id),
 codigo VARCHAR(120) NOT NULL, nome VARCHAR(255) NOT NULL, ativo BOOLEAN NOT NULL,
 PRIMARY KEY (organizacao_id,id), UNIQUE(organizacao_id,codigo));
CREATE TABLE fornecedores (id UUID NOT NULL, organizacao_id UUID NOT NULL REFERENCES organizacoes_projecao(id),
 codigo VARCHAR(120) NOT NULL, nome VARCHAR(255) NOT NULL, ativo BOOLEAN NOT NULL,
 PRIMARY KEY (organizacao_id,id), UNIQUE(organizacao_id,codigo));
CREATE TABLE itens (id UUID NOT NULL, organizacao_id UUID NOT NULL REFERENCES organizacoes_projecao(id),
 codigo VARCHAR(120) NOT NULL, descricao VARCHAR(255) NOT NULL, categoria_id UUID NOT NULL,
 unidade_medida_id UUID NOT NULL, rastreabilidade VARCHAR(30) NOT NULL,
 estoque_minimo NUMERIC(19,4) NOT NULL CHECK(estoque_minimo>=0),
 estoque_maximo NUMERIC(19,4) NOT NULL CHECK(estoque_maximo>=estoque_minimo), ativo BOOLEAN NOT NULL,
 PRIMARY KEY(organizacao_id,id), UNIQUE(organizacao_id,codigo),
 FOREIGN KEY(organizacao_id,categoria_id) REFERENCES categorias(organizacao_id,id),
 FOREIGN KEY(organizacao_id,unidade_medida_id) REFERENCES unidades_medida(organizacao_id,id),
 CHECK(rastreabilidade IN ('NENHUM','LOTE','LOTE_VALIDADE','NUMERO_SERIE')));
