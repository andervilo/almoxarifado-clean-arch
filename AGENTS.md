# AGENTS.md

## Visão geral

Este repositório implementa um sistema de almoxarifado com arquitetura híbrida:
- Backend em Java 21 + Spring Boot
- Monólito modular com Spring Modulith
- Frontend em Angular 20
- Banco PostgreSQL
- Flyway para migrações
- Docker Compose para ambiente local
- Keycloak para autenticação e autorização

O objetivo principal é desenvolver um MVP funcional, com foco em estoque, rastreabilidade, requisições e inventário.

---

## Regras de arquitetura

### Backend
- seguir Clean Architecture
- domínio independente de framework
- camada de aplicação orquestrando casos de uso
- infraestrutura responsável por controllers, JPA, segurança e integrações
- módulos não devem chamar beans de outros módulos diretamente
- comunicação entre módulos via eventos de integração
- uso obrigatório de Spring Modulith

### Frontend
- Angular 20
- organização por features
- estrutura sugerida:
  - core/
  - shared/
  - features/
  - layouts/
- usar standalone components
- lazy loading por feature
- evitar lógica de negócio no template
- centralizar autenticação e token em serviços e interceptors

---

## Convenções de desenvolvimento

### Organização de pastas

```text
backend/
  src/
    main/
      java/
        com.almoxarifado/
          shared/
          organizacao/
          catalogo/
          estrutura/
          estoque/
          requisicao/
          inventario/
          alerta/

frontend/
  src/
    app/
      core/
      shared/
      features/
      layouts/
```

### Padrões esperados
- nomes em português na linguagem ubíqua do domínio
- classes e arquivos com nomes claros e específicos
- DTOs separados da entidade do domínio
- casos de uso com responsabilidades bem definidas
- regra de negócio no domínio e não na camada web
- transações em operações de escrita, principalmente em estoque

---

## Autenticação e autorização

- autenticação centralizada no Keycloak
- uso de JWT no backend
- backend deve atuar como resource server
- frontend deve autenticar via OIDC/OAuth2
- toda rota sensível deve exigir guard de autenticação
- permissões por perfil/role
- dados por organização devem ser isolados por `organizacao_id`

---

## Banco de dados e migrações

- PostgreSQL como banco principal
- Flyway para versionamento e migrações
- cada módulo deve ter sua pasta de migração
- scripts seguem padrão: `V<versao>__descricao.sql`
- não alterar schema diretamente em ambiente de produção
- regras críticas de estoque devem ser garantidas via transações e testes

---

## Docker e ambiente local

- ambiente local deve funcionar com Docker Compose
- aplicar `docker compose up --build`
- incluir ao menos:
  - app
  - postgres
  - keycloak
- manter configuração de ambiente separada por profiles

---

## Testes obrigatórios

- testes unitários para regras do domínio
- testes de integração para persistência e workflows
- testes de API para endpoints críticos
- testes de arquitetura com ArchUnit
- testes de módulos com Spring Modulith Test
- testes frontend com Jasmine/Karma para componentes e serviços essenciais

---

## Regras de negócio prioritárias

1. saldo nunca pode ficar negativo
2. transferências devem ser atômicas
3. itens inativos não podem ser movimentados
4. lotes vencidos ficam bloqueados para saída normal
5. itens com validade usam regra FEFO por padrão
6. itens sem validade usam FIFO por padrão
7. movimentações não devem ser apagadas; correções via estorno ou ajuste
8. toda operação sensível deve ser auditada

---

## Entregas por prioridade

### Fase 1
- infraestrutura e ambiente local
- Docker, PostgreSQL, Flyway, Keycloak

### Fase 2
- autenticação e autorização
- organização e usuários

### Fase 3
- catálogo e estrutura física

### Fase 4
- estoque e regras críticas

### Fase 5
- requisições e entregas

### Fase 6
- inventário

### Fase 7
- alertas e dashboard

### Fase 8
- frontend Angular, testes e documentação final

---

## Critérios de conclusão

A implementação está concluída quando:
- backend e frontend compilaram com sucesso
- o ambiente roda em Docker Compose
- autenticação via Keycloak funciona
- estoque principal está consistente
- fluxos de requisição, inventário e alertas funcionam
- as migrações do Flyway aplicam corretamente
- a documentação e os testes básicos estão em dia

---

## Observação final

Sempre priorize a correção da regra de negócio antes de beleza de interface. O núcleo do sistema é o estoque e sua rastreabilidade. Qualquer decisão de arquitetura ou implementação deve preservar consistência, segurança e integridade dos dados.
