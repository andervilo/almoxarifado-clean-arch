-- Executado na inicialização e pelo serviço postgres-init para volumes existentes.
-- O Keycloak usa um database próprio para não misturar suas tabelas com as da aplicação.
SELECT 'CREATE DATABASE keycloak'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'keycloak')
\gexec
