CREATE TABLE itens_estoque (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(120) NOT NULL UNIQUE,
    descricao VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    saldo NUMERIC(19, 4) NOT NULL DEFAULT 0
);

CREATE TABLE lotes_estoque (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL,
    codigo VARCHAR(120) NOT NULL,
    validade DATE NULL,
    quantidade NUMERIC(19, 4) NOT NULL,
    CONSTRAINT fk_lote_item FOREIGN KEY (item_id) REFERENCES itens_estoque (id),
    CONSTRAINT uk_lote_item_codigo UNIQUE (item_id, codigo)
);

CREATE TABLE movimentacoes_estoque (
    id BIGSERIAL PRIMARY KEY,
    item_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    quantidade NUMERIC(19, 4) NOT NULL,
    codigo_lote VARCHAR(120) NULL,
    data_hora TIMESTAMP NOT NULL,
    CONSTRAINT fk_movimentacao_item FOREIGN KEY (item_id) REFERENCES itens_estoque (id)
);

CREATE INDEX idx_lotes_item_validade ON lotes_estoque (item_id, validade);
CREATE INDEX idx_movimentacoes_item_data ON movimentacoes_estoque (item_id, data_hora);
