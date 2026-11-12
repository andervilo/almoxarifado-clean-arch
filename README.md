# Almoxarifado

Sistema de almoxarifado para gestão de estoque com rastreabilidade, requisições, inventário e alertas.

## Stack

- Java 21
- Spring Boot
- Spring Modulith
- PostgreSQL
- Flyway
- Docker Compose
- Keycloak
- Angular 20

## Objetivo

Implementar um MVP funcional para controle de materiais com foco em:
- entradas e saídas de estoque
- rastreabilidade por lote, validade e número de série
- requisições
- inventário
- alertas de estoque mínimo e validade
- autenticação por perfil e organização

## Arquitetura

### Backend
- monólito modular em Java/Spring Boot
- módulos por domínio:
  - shared
  - organizacao
  - catalogo
  - estrutura
  - estoque
  - requisicao
  - inventario
  - alerta
- comunicação entre módulos via eventos
- Clean Architecture + DDD

### Frontend
- Angular 20
- estrutura em `core`, `shared`, `features` e `layouts`
- standalone components
- lazy loading por feature

### Infraestrutura
- PostgreSQL como banco principal
- Flyway para versionamento do banco
- Docker Compose para ambiente local
- Keycloak para autenticação e autorização

---

## Requisitos

- Docker
- Docker Compose
- Java 21
- Node.js 20+
- npm ou pnpm

---

## Execução local

### Backend e infraestrutura

```bash
docker compose up --build
```

Isso deve levantar:
- aplicação backend
- PostgreSQL
- Keycloak

O Compose constrói as imagens de PostgreSQL e Keycloak com os scripts e o realm
incluídos, sem bind mounts de caminhos locais. O serviço `postgres-init` cria o
database `keycloak` de forma idempotente, inclusive em volumes existentes. O backend
só inicia depois de PostgreSQL e Keycloak estarem prontos.

Para executar apenas a infraestrutura e iniciar o backend pela IDE:

```bash
docker compose up -d --build postgres postgres-init keycloak
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

O profile `docker` usa os nomes dos serviços na rede interna; `local` usa as portas
publicadas no host. Os tokens sempre têm issuer público
`http://localhost:8181/realms/almoxarifado`, enquanto o backend em Docker busca as
chaves em `http://keycloak:8080`. A audience exigida é `almoxarifado-backend`.

### Volumes de versões anteriores

Não é necessário remover o volume PostgreSQL. Tabelas antigas em `public` são
preservadas; as novas migrações usam schemas e históricos próprios por módulo,
sem `baseline-on-migrate`. Dados de estoque que já existam em `public` precisam de
migração explícita antes de operar na nova estrutura; não são copiados automaticamente.

Ao mudar o Keycloak do database `almoxarifado` para `keycloak`, o realm de
desenvolvimento é importado no banco novo. Configurações e contas antigas continuam
no banco anterior. A importação de inicialização não sobrescreve um realm que já
exista no banco de destino; alterações posteriores exigem atualização pelo console
ou pela API administrativa do Keycloak.

### Frontend

```bash
cd frontend
npm install
npm start
```

---

## URLs locais

- Backend: http://localhost:8080
- Keycloak: http://localhost:8181
- PostgreSQL: localhost:6432
- Swagger/OpenAPI: http://localhost:8080/swagger-ui/index.html
- Frontend: http://localhost:4200

## Validação da fundação

```bash
cd backend
mvn clean verify
```

Os testes de integração exigem Docker e usam PostgreSQL 16 via Testcontainers.
Cobrem migrações em banco ocupado por tabelas legadas, reinicialização sem perda
de dados e registro transacional de eventos do Modulith. ArchUnit e
`ApplicationModules.verify()` verificam as fronteiras arquiteturais.

Com o Compose em execução, valide tokens reais e os cinco perfis de desenvolvimento:

```powershell
./scripts/validar-fundacao.ps1
```

No Windows com execução de scripts desabilitada, use uma liberação apenas para o
processo de validação:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/validar-fundacao.ps1
```

O script verifica health checks, OpenAPI, sessão autenticada em
`GET /api/v1/auth/me`, organização do token, permissões do Actuator e rejeição de
tokens inválidos ou sem a audience da API. Não imprime tokens. O grant de senha é
usado apenas nesse teste local; a SPA deverá usar Authorization Code com PKCE.
As chamadas do script usam `127.0.0.1` para evitar diferenças de resolução IPv6
entre Windows e Docker Linux; o issuer do token continua sendo `localhost`.

As credenciais e a organização de demonstração estão em
[docs/keycloak-users.md](docs/keycloak-users.md). O endpoint de sessão confirma os
claims. Os novos repositórios e casos de uso aplicam isolamento por organização;
os workflows e seus limites estão descritos em `docs/BACKEND_MVP.md`.

---

## Estrutura sugerida

```text
backend/
frontend/
  src/
    app/
      core/
      shared/
      features/
      layouts/

docs/
  intent.md
  spec.md

AGENTS.md
README.md
PLANO_EXECUCAO.md
```

---

## Regras principais

- saldo nunca pode ficar negativo
- transferências devem ser atômicas
- itens inativos não podem ser movimentados
- lotes vencidos ficam bloqueados para saída normal
- itens com validade usam FEFO
- itens sem validade usam FIFO
- movimentações nunca são removidas; correções via estorno ou ajuste
- toda operação sensível deve ser auditada

---

## Backend operacional

Os módulos de organização, catálogo, estrutura, estoque, requisição, inventário e
alertas têm persistência, casos de uso e endpoints. O estoque inclui saldo por
local/lote/série, custo médio, reserva, transferência e estorno. Os workflows entre
módulos usam eventos do Modulith.

Consulte [docs/BACKEND_MVP.md](docs/BACKEND_MVP.md) para rotas, regras, estados,
testes e recursos da especificação ainda pendentes. Execute o fluxo com JWT real:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/validar-backend.ps1
```

---

## Observação

Este repositório segue as convenções definidas em [AGENTS.md](AGENTS.md) e no material de especificação em [docs/spec.md](docs/spec.md).
