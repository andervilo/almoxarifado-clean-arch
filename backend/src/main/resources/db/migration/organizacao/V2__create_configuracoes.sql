CREATE TABLE configuracoes (
    organizacao_id UUID PRIMARY KEY REFERENCES organizacoes(id),
    dias_alerta_validade INTEGER NOT NULL DEFAULT 30 CHECK(dias_alerta_validade BETWEEN 1 AND 365)
);
