CREATE TABLE provisionamentos_identidade(organizacao_id UUID NOT NULL,id UUID NOT NULL,ator VARCHAR(255) NOT NULL,dados TEXT NOT NULL,
 status VARCHAR(30) NOT NULL,keycloak_id UUID,erro VARCHAR(120),criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(organizacao_id,id),FOREIGN KEY(organizacao_id) REFERENCES organizacoes(id));
CREATE TABLE identidades_gerenciadas(organizacao_id UUID NOT NULL,keycloak_id UUID NOT NULL,ultimo_provisionamento UUID NOT NULL,
 PRIMARY KEY(organizacao_id,keycloak_id),FOREIGN KEY(organizacao_id,ultimo_provisionamento) REFERENCES provisionamentos_identidade(organizacao_id,id));
