# Usuários iniciais do Keycloak

## Realm

- nome: `almoxarifado`
- PostgreSQL exposto em `localhost:6432` no ambiente local
- Keycloak exposto em `http://localhost:8181`

## Usuários de desenvolvimento

| Perfil | Username | Senha | Observação |
|---|---|---|---|
| ADMIN_PLATAFORMA | `admin.plataforma` | `Admin@123` | administrador global |
| ADMIN_ORGANIZACAO | `admin.organizacao` | `OrgAdmin@123` | administrador da organização |
| ALMOXARIFE | `almoxarife` | `Almoxarife@123` | operação de estoque |
| REQUISITANTE | `requisitante` | `Requisitante@123` | solicita materiais |
| GESTOR | `gestor` | `Gestor@123` | acompanhamento executivo |

## Papéis esperados

- `ADMIN_PLATAFORMA`
- `ADMIN_ORGANIZACAO`
- `ALMOXARIFE`
- `REQUISITANTE`
- `GESTOR`

## Observações

- Esses usuários são para ambiente de desenvolvimento e teste.
- Em produção, as credenciais devem ser alteradas e armazenadas em um gerenciador de segredos.
- Cada usuário deve possuir a role adequada conforme o perfil do domínio.

## Como criar via Keycloak

1. Acesse o console do Keycloak em `http://localhost:8081`.
2. Entre com `admin / admin`.
3. Crie o realm `almoxarifado`.
4. Crie os roles listados acima.
5. Crie os usuários com as senhas sugeridas.
6. Atribua a role correta para cada usuário.
7. Configure o client do backend e o client do frontend conforme o projeto.
