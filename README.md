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

## Próxima etapa

- Implementar a base do backend
- Configurar Docker e Keycloak
- Implementar autenticação e autorização
- Implementar módulo de estoque
- Evoluir para requisições, inventário e alertas

---

## Observação

Este repositório segue as convenções definidas em [AGENTS.md](AGENTS.md) e no material de especificação em [docs/spec.md](docs/spec.md).
