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

## Administração de contas pelo backend

O client confidencial `almoxarifado-admin` usa service account com as roles
`manage-users`, `view-users` e `view-realm` de `realm-management`. Ele não recebe
permissões de administração de clients ou do realm. O backend usa client
credentials somente na API administrativa; as APIs públicas continuam resource
server com validação de audience e issuer.

Em instalações existentes de desenvolvimento, execute:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/configurar-keycloak-dev.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/validar-identidades.ps1
```

A configuração preserva contas existentes, adiciona service account, atributos
protegidos e mappers. Não rotaciona segredos: se o client existir com segredo
diferente, informe `KEYCLOAK_ADMIN_CLIENT_SECRET`. O script usa as credenciais
administrativas locais ou `KEYCLOAK_ADMIN`/`KEYCLOAK_ADMIN_PASSWORD` do ambiente.

O provisionamento fica desabilitado por padrão fora do Compose. Configure
`KEYCLOAK_ADMIN_ENABLED=true`, `KEYCLOAK_ADMIN_BASE_URL`,
`KEYCLOAK_ADMIN_CLIENT_ID` e `KEYCLOAK_ADMIN_CLIENT_SECRET`. No Compose local,
ele está habilitado, com segredo de demonstração. Em produção, use segredo
próprio e transporte HTTPS entre serviços.

`organizacao_id`, `almoxarifados`, `escopo_restrito` e `provisionamento_id` são
atributos administrativos. `almoxarifados` gera array de UUIDs no JWT;
`escopo_restrito` é booleano e evita que uma lista vazia omitida pelo Keycloak
seja interpretada como acesso irrestrito. Admins do tenant não são limitados
por essa lista. Alterações encerram sessões, mas tokens existentes duram até
expirar (cinco minutos na configuração local).

Novas contas ficam sem senha e exigem `UPDATE_PASSWORD`. A administração deve
concluir o onboarding no Keycloak; o backend não recebe senhas nem dispara
e-mails. O script de validação define senha aleatória diretamente no provedor
apenas para a conta que acabou de criar, testa seu JWT e termina desabilitando
essa conta pelo backend, preservando a auditoria. Não use esse script em produção.

Referências: [API administrativa Keycloak 24.0.5](https://www.keycloak.org/docs-api/24.0.5/rest-api/index.html)
e [administração de service accounts](https://www.keycloak.org/docs/24.0.5/server_admin/index.html).
