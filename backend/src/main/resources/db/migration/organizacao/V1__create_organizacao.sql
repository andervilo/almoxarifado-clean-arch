CREATE TABLE organizacoes (
    id UUID PRIMARY KEY, nome VARCHAR(255) NOT NULL, codigo VARCHAR(120) NOT NULL UNIQUE,
    ativa BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE setores (
    organizacao_id UUID NOT NULL REFERENCES organizacoes(id), codigo VARCHAR(120) NOT NULL,
    nome VARCHAR(255) NOT NULL, PRIMARY KEY (organizacao_id, codigo)
);
CREATE TABLE usuarios (
    organizacao_id UUID NOT NULL REFERENCES organizacoes(id), codigo VARCHAR(255) NOT NULL,
    nome VARCHAR(255) NOT NULL, email VARCHAR(255) NOT NULL, perfil VARCHAR(40) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE, PRIMARY KEY (organizacao_id, codigo),
    UNIQUE (organizacao_id, email),
    CHECK (perfil IN ('ADMIN_ORGANIZACAO','ALMOXARIFE','REQUISITANTE','GESTOR'))
);
