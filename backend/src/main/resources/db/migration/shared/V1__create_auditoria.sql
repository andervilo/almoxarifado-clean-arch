CREATE TABLE auditoria (
    id UUID PRIMARY KEY,
    organizacao_id UUID NOT NULL,
    usuario_id VARCHAR(255) NOT NULL,
    acao VARCHAR(120) NOT NULL,
    recurso VARCHAR(255) NOT NULL,
    ocorrido_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_auditoria_organizacao_data ON auditoria (organizacao_id, ocorrido_em);
CREATE FUNCTION impedir_alteracao_auditoria() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Registros de auditoria sao imutaveis';
END;
$$;
CREATE TRIGGER auditoria_imutavel BEFORE UPDATE OR DELETE OR TRUNCATE ON auditoria
FOR EACH STATEMENT EXECUTE FUNCTION impedir_alteracao_auditoria();
