# Plano de implementação do projeto Almoxarifado

## Visão geral

Este plano define a sequência técnica de implementação do sistema, priorizando a construção do MVP em arquitetura modular, com backend em Java/Spring Boot, frontend em Angular 20, PostgreSQL, Flyway, Docker e Keycloak.

A ideia é implementar primeiro o núcleo de negócio (estoque), depois expandir para requisições, inventário, alertas e interface.

---

## Objetivo do MVP

Entregar uma solução funcional para:
- cadastro de organização, usuários e permissões;
- cadastro de itens e estrutura física;
- entrada, saída, transferência e histórico de estoque;
- rastreabilidade por lote, validade e número de série;
- requisições e inventário;
- autenticação via Keycloak;
- dashboard básico e telas operacionais no Angular.

---

## Ordem de implementação

## 1. Base do projeto e infraestrutura

### Tarefas
- criar estrutura do projeto backend com Spring Boot
- configurar Gradle/Maven
- criar módulos do monólito com Spring Modulith
- configurar PostgreSQL
- configurar Flyway
- criar Dockerfile
- criar docker-compose.yml
- incluir Keycloak, backend e banco no ambiente local
- configurar profiles (`local`, `docker`, `test`)

### Entregáveis
- projeto backend inicial compilando
- ambiente local em containers
- banco e aplicação subindo corretamente
- Keycloak pronto para autenticação

### Critérios de aceite
- `docker compose up --build` funciona
- backend sobe e conecta no PostgreSQL
- Flyway executa migrations sem erros
- Keycloak responde em localhost

---

## 2. Autenticação e autorização

### Tarefas
- configurar Spring Security
- integrar com Keycloak via OIDC/OAuth2
- criar realm `almoxarifado`
- configurar clients e roles
- criar claims de organização e perfil
- criar `AuthGuard` no Angular
- criar `AuthService` e `KeycloakService`
- criar `HttpInterceptor` para JWT
- criar página de login e fluxo de logout
- criar página de acesso negado

### Entregáveis
- login funcional
- sessão segura com JWT
- redirecionamento por perfil
- rotas protegidas no Angular

### Critérios de aceite
- usuários autenticados conseguem acessar a app
- pessoas sem perfil não entram em telas restritas
- token aparece no header das requisições
- sessão expirada é tratada corretamente

---

## 3. Módulo de organização e acesso

### Tarefas
- criar `Organizacao`
- criar `Usuario`
- criar `Perfil`
- criar `Setor`
- criar `ConfiguracaoOrganizacao`
- implementar endpoints de cadastro e consulta
- criar DTOs de request/response
- criar use cases de criação e atualização
- criar eventos de integração para organização e setores

### Entregáveis
- organização configurável
- usuários e perfis cadastrados
- setores vinculados à organização

### Critérios de aceite
- organização pode ser criada com perfil administrativo
- usuários pertencem a uma organização
- dados do tenant são isolados

---

## 4. Módulo de catálogo e estrutura física

### Tarefas
- criar `Item`
- criar `Categoria`
- criar `UnidadeMedida`
- criar `Fornecedor`
- criar `Almoxarifado`
- criar `LocalArmazenamento`
- implementar regras de rastreabilidade
- criar endpoints do catálogo
- criar endpoints de almoxarifado e localização
- criar eventos `ItemCadastrado`, `AlmoxarifadoCriado`, `LocalCriado`

### Entregáveis
- cadastro de itens funcionando
- locais e almoxarifados cadastrados
- itens com tipos de rastreabilidade configurados

### Critérios de aceite
- item pode ser cadastrado com estoque mínimo e máximo
- item pode ser rastreado por lote, validade ou número de série
- almoxarifado e local estão vinculados corretamente

---

## 5. Módulo de estoque (núcleo do sistema)

### Tarefas
- criar `MovimentacaoEstoque`
- criar `LinhaMovimentacao`
- criar `Lote`
- criar `NumeroSerie`
- criar `SaldoItemAlmoxarifado`
- implementar casos de uso:
  - registrar entrada
  - registrar saída
  - transferir material
  - consultar saldo
  - consultar histórico
- implementar regras:
  - saldo não negativo
  - FEFO para validade
  - FIFO para itens sem validade
  - bloqueio de lote vencido
  - custo médio ponderado
- aplicar transações e auditoria
- criar eventos de estoque para outros módulos

### Entregáveis
- movimentação funcional
- histórico e saldo por item/almoxarifado/local
- rastreabilidade consistente

### Critérios de aceite
- entrada e saída atualizam saldo corretamente
- saída de item vencido é bloqueada
- transferência é atômica
- operação crítica não deixa saldo inconsistente

---

## 6. Módulo de requisições

### Tarefas
- criar `Requisicao`
- criar `LinhaRequisicao`
- criar regras de solicitação e reserva
- implementar criação de requisição
- implementar separação e entrega
- conectar com módulo de estoque
- criar eventos `ReservaEfetuada`, `ReservaLiberada`, `BaixaRequisicaoEfetuada`

### Entregáveis
- fluxo de requisição funcional
- requisição com status operacional
- baixa no estoque refletida corretamente

### Critérios de aceite
- requisitante consegue criar pedido
- almoxarife consegue separar e entregar
- saldo real é atualizado no processo

---

## 7. Módulo de inventário

### Tarefas
- criar `Inventario`
- criar `ContagemInventario`
- implementar contagem física
- comparar saldo sistema com contagem
- aplicar ajuste com justificativa
- conectar com módulo de estoque

### Entregáveis
- processo de inventário funcionando
- divergências detectadas e ajustadas

### Critérios de aceite
- contagem gera diferença corretamente
- ajuste exige justificativa e permissão adequada
- histórico de inventário fica preservado

---

## 8. Módulo de alertas

### Tarefas
- implementar monitoramento de estoque mínimo
- implementar alerta de validade próxima
- implementar alerta de lote vencido
- expor notificações para dashboard
- criar eventos de alerta

### Entregáveis
- alertas visíveis na interface
- listagem de itens críticos

### Critérios de aceite
- item abaixo do mínimo é destacado
- lote próximo do vencimento gera aviso
- lote vencido não pode sair por requisição normal

---

## 9. Frontend Angular 20

### Tarefas
- criar estrutura de app com `core`, `shared`, `features`, `layouts`
- criar rota inicial de login
- criar dashboard inicial
- criar feature `auth`
- criar feature `estoque`
- criar feature `requisicoes`
- criar feature `inventario`
- criar feature `configuracoes`
- criar feature `relatorios`
- criar components reutilizáveis
- implementar API services para backend
- implementar interceptors para JWT
- implementar guards e tratamento de erros

### Entregáveis
- app Angular rodando
- telas principais funcionando
- login com Keycloak integrado
- consultas e ações do backend consumidas

### Critérios de aceite
- login funciona corretamente
- rotas protegidas respeitam perfil
- dashboard exibe indicadores básicos
- estoque e requisizações operam na interface

---

## 10. Testes e qualidade

### Tarefas
- escrever testes unitários do domínio
- testar regras de estoque
- criar testes de integração com Testcontainers
- criar testes de API com MockMvc
- validar regras arquiteturais com ArchUnit
- validar módulos com Spring Modulith Test
- testar frontend com Jasmine/Karma

### Entregáveis
- suíte automatizada para regras críticas
- validação da arquitetura
- validação de endpoints e componentes

### Critérios de aceite
- módulos sem violação de dependência
- regras críticas cobertas por testes
- aplicação estável e consistente

---

## 11. Documentação e observabilidade

### Tarefas
- documentar endpoints com Swagger/OpenAPI
- configurar Actuator
- expor health checks
- padronizar respostas de erro
- registrar logs estruturados
- documentar flows principais e regras de negócio

### Entregáveis
- documentação de API pronta
- health checks e monitoramento básico
- logs úteis para operação

### Critérios de aceite
- todas as operações principais documentadas
- API responde com padrões claros
- aplicação monitorável

---

## 12. Fase final de validação

### Tarefas
- executar suíte de testes
- validar build do backend
- validar build do frontend
- validar Docker Compose completo
- validar login via Keycloak
- validar fluxo principal do estoque
- validar requisição e inventário
- verificar documentação e procedimentos de uso

### Critérios de aceite
- backend e frontend compilam
- a solução funciona em ambiente local completo
- o fluxo principal do sistema está operando
- documentação e ambiente estão alinhados

---

## Sequência recomendada de execução

1. infraestrutura + docker + db + flyway
2. Keycloak + autenticação
3. organização e usuários
4. catálogo + estrutura física
5. estoque + regras de negócio
6. requisições
7. inventário
8. alertas
9. frontend Angular
10. testes finais
11. documentação e validação

---

## Entregas por marco

### Marco 1 — Base operacional
- backend rodando
- banco funcionando
- Flyway ativo
- Keycloak autenticando

### Marco 2 — Núcleo de estoque pronto
- cadastro de itens e entradas/saídas operando
- saldo consistente
- lote e validade funcionando

### Marco 3 — Sistema operacional MVP
- requisições e inventário ativos
- alertas funcionando
- frontend com telas principais

### Marco 4 — Entrega final
- app funcionando em Docker
- testes automáticos validados
- documentação pronta

---

## Observações finais

Este plano prioriza a construção do núcleo de negócio antes da interface, porque o estoque é o coração do sistema. Depois que as regras de negócio estiverem consistentes, a criação do frontend e da autenticação fica mais segura, com menos retrabalho e menos risco de inconsistência funcional.
