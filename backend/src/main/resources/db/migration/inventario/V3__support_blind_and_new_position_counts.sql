ALTER TABLE inventarios ADD COLUMN contagem_cega BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE contagens ADD CONSTRAINT uk_contagem_posicao_natural
 UNIQUE(organizacao_id,inventario_id,item_id,local_id,lote,serie);
