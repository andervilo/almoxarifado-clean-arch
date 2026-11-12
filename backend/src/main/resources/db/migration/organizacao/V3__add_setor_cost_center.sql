ALTER TABLE setores ADD COLUMN centro_custo VARCHAR(120) NOT NULL DEFAULT '';
ALTER TABLE setores ADD COLUMN snapshot_id UUID;
UPDATE setores SET snapshot_id=md5(organizacao_id::text||':'||codigo)::uuid;
ALTER TABLE setores ALTER COLUMN snapshot_id SET NOT NULL;
ALTER TABLE setores ADD CONSTRAINT uk_setor_snapshot UNIQUE(organizacao_id,snapshot_id);
