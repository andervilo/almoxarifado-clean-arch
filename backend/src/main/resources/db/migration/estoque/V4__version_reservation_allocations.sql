CREATE TABLE revisoes_reserva(organizacao_id UUID NOT NULL,id UUID NOT NULL,requisicao_id UUID NOT NULL,item_id UUID NOT NULL,
 revisao BIGINT NOT NULL,PRIMARY KEY(organizacao_id,id),UNIQUE(organizacao_id,requisicao_id,item_id));
INSERT INTO revisoes_reserva(organizacao_id,id,requisicao_id,item_id,revisao)
 SELECT r.organizacao_id,md5(r.organizacao_id::text||':'||r.requisicao_id||':'||p.item_id)::uuid,r.requisicao_id,p.item_id,1
 FROM reservas r JOIN posicoes p ON p.organizacao_id=r.organizacao_id AND p.id=r.posicao_id
 GROUP BY r.organizacao_id,r.requisicao_id,p.item_id;
