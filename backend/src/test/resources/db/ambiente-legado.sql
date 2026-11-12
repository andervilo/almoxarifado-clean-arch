-- Simula um banco já ocupado por tabelas do Keycloak antes da separação.
CREATE TABLE public.realm (id VARCHAR(255) PRIMARY KEY, name VARCHAR(255) NOT NULL);
INSERT INTO public.realm (id, name) VALUES ('realm-legado', 'almoxarifado-legado');
