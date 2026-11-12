-- O prototipo permanece preservado, sem endpoints de escrita novos.
-- Protege tambem o historico legado, alem das operacoes transacionais da V2.
CREATE TRIGGER movimentacoes_legadas_imutaveis
BEFORE UPDATE OR DELETE OR TRUNCATE ON movimentacoes_estoque
FOR EACH STATEMENT EXECUTE FUNCTION impedir_alteracao_movimento();
