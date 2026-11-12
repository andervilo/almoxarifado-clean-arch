# Usuários iniciais do Keycloak

## Realm

- nome: `almoxarifado`
- PostgreSQL exposto em `localhost:6432` no ambiente local
- Keycloak exposto em `http://localhost:8181`
- database exclusivo do Keycloak: `keycloak` (a aplicação usa `almoxarifado`)
- importação automática: `docker/keycloak/import/almoxarifado-realm.json`
- organização de demonstração: `0190f5a0-0000-7000-8000-000000000001`

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

## Provisionamento e verificação

Execute `docker compose up --build`. Em um banco de destino novo, o Keycloak importa
automaticamente o realm, os clients, as roles e os usuários acima. O console fica
em `http://localhost:8181`, com `admin / admin` por padrão no ambiente local.

- `almoxarifado-backend`: resource server, não emite tokens nem mantém senhas.
- `almoxarifado-frontend`: client público, Authorization Code + PKCE S256;
  redirecionamento e CORS em `http://localhost:4200`.
- Access tokens incluem a audience `almoxarifado-backend` e o claim UUID
  `organizacao_id`. Somente administradores do Keycloak podem editar esse atributo.
- O grant de senha está habilitado apenas para os testes locais com o script
  `scripts/validar-fundacao.ps1`; a interface deve usar PKCE.

A importação não atualiza realms existentes. Se o banco `keycloak` já contiver o
realm, ajuste clients, mappers e usuários pelo console/API administrativa,
preservando os dados. O administrador da plataforma deve cadastrar a organização
do token por `POST /api/v1/organizacoes`. O script `scripts/validar-backend.ps1`
faz esse cadastro inicial para a organização de demonstração, se necessário.
