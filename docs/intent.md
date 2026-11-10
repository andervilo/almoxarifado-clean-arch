# Intent — Sistema de Almoxarifado

> Documento de intenção: descreve **o quê** e **por quê** do sistema, antes de decidir **como**.
> Status: rascunho v2 · Última atualização: 2026-11-09

---

## 1. Visão

Criar um sistema de almoxarifado **genérico e configurável**, capaz de atender **diferentes tipos de empresas** (indústria, comércio, serviços, saúde, construção, órgãos públicos etc.), oferecendo **controle confiável, rastreável e em tempo real** sobre os materiais — do recebimento à saída — com suporte a **um ou vários almoxarifados** por organização e **rastreabilidade por lote, validade e número de série**.

## 2. Problema

- O controle de estoque costuma ser feito em planilhas ou papel, sujeito a erros e desatualização.
- Não se sabe com precisão **quanto** há de cada item, **onde** está, **de qual lote** e **quem** retirou.
- Materiais vencem no estoque por falta de controle de validade.
- Itens de alto valor ou controlados (equipamentos, ferramentas, medicamentos) não são rastreados individualmente.
- Compras são feitas tarde demais (falta de material) ou cedo demais (capital parado).
- Não há histórico confiável para auditoria, inventário ou prestação de contas.

## 3. Objetivos

| # | Objetivo | Como medir |
|---|----------|------------|
| O1 | Saldo de estoque sempre correto | Divergência no inventário físico < 2% |
| O2 | Rastreabilidade total das movimentações | 100% das entradas/saídas com usuário, data, motivo e lote/série quando aplicável |
| O3 | Evitar perdas por vencimento | Alertas de validade; redução de descarte por vencimento |
| O4 | Evitar falta de material | Alertas de estoque mínimo; redução de rupturas |
| O5 | Atender empresas de perfis diferentes | Configuração por organização sem alteração de código |
| O6 | Apoiar decisões de compra | Relatórios de consumo e curva ABC disponíveis |

## 4. Usuários (personas)

- **Almoxarife** — registra entradas, saídas, transferências e inventários; atende requisições.
- **Requisitante** (colaborador/setor) — solicita materiais e acompanha o status do pedido.
- **Gestor** — acompanha indicadores, consumo e custos por setor/almoxarifado.
- **Compras** — consulta itens abaixo do mínimo e histórico de consumo para planejar aquisições.
- **Administrador da organização** — gerencia usuários, permissões, almoxarifados e configurações da empresa.
- **Administrador da plataforma** — gerencia as organizações cadastradas *(aplicável se o modelo for multi-tenant — ver seção 12)*.

## 5. Escopo

### 5.1 Dentro do escopo (MVP)

1. **Organização e estrutura**
   - Cadastro da organização (empresa) e suas configurações
   - **Um ou vários almoxarifados** por organização
   - Locais de armazenagem por almoxarifado (corredor → prateleira → posição)
   - Setores / centros de custo
2. **Cadastros**
   - Itens/materiais: código, descrição, unidade de medida, categoria, estoque mínimo/máximo, foto opcional
   - **Tipo de rastreabilidade por item**: nenhum, **por lote**, **por lote + validade** ou **por número de série**
   - Categorias / grupos
   - Fornecedores
   - Usuários e perfis de acesso (por organização e, opcionalmente, por almoxarifado)
3. **Movimentações**
   - Entrada (compra, devolução, doação, ajuste) com documento de referência (ex.: nº da nota fiscal)
     - informa lote, data de fabricação/validade ou números de série conforme o tipo do item
   - Saída (requisição, consumo, perda/avaria, vencimento, ajuste)
   - Transferência entre locais e **entre almoxarifados**
   - Estorno de movimentação (nunca apagar — sempre lançamento inverso)
4. **Requisições**
   - Requisitante cria pedido → separação → entrega → confirmação de recebimento
   - Fluxo de aprovação: **adiado** (ver seção 12); o modelo deve permitir incluí-lo depois sem retrabalho
5. **Consulta de estoque**
   - Saldo por item, por almoxarifado, por local e **por lote/série**
   - Histórico (kardex) de cada item e rastreio de um lote/série específico (de onde veio, para onde foi)
6. **Alertas**
   - Item abaixo do estoque mínimo (por almoxarifado)
   - Lotes com validade próxima (antecedência configurável) e lotes vencidos
7. **Inventário**
   - Contagem física (geral ou parcial por almoxarifado/local), comparação com o sistema e ajuste justificado
   - Contagem por lote/série para itens rastreados
8. **Relatórios básicos**
   - Posição de estoque, movimentações por período, consumo por setor, itens críticos, lotes a vencer

### 5.2 Fora do escopo (por enquanto)

- Fluxo de **aprovação de requisições** (decisão adiada)
- **Integrações** com ERP, sistemas contábeis ou fiscais (emissão/importação de NF-e)
- Módulo completo de compras (cotação, pedido de compra, aprovação financeira)
- Aplicativo mobile nativo (o front-end consumirá a API e deve ser responsivo)
- Controle de patrimônio (bens permanentes com depreciação)

> Esses itens podem entrar em fases futuras — ver seção 10.

## 6. Regras de negócio essenciais

- **RN01** — O saldo de um item **nunca** pode ficar negativo — nem no total, nem por almoxarifado, local ou lote.
- **RN02** — Toda movimentação registra: item, quantidade, tipo, almoxarifado/local, usuário, data/hora, motivo/documento e lote/série quando aplicável.
- **RN03** — Movimentações não são editadas nem excluídas; correções são feitas por **estorno** ou **ajuste** justificado.
- **RN04** — Itens controlados **por lote** exigem informar o lote em toda entrada e saída.
- **RN05** — Itens controlados **por validade** exigem data de validade na entrada; a saída segue **FEFO** (vence primeiro, sai primeiro) por padrão, permitindo escolha manual com justificativa.
- **RN06** — Itens sem validade seguem **FIFO**.
- **RN07** — Itens controlados **por número de série** são movimentados unitariamente; cada número de série é **único por item na organização** e só pode estar em um local por vez.
- **RN08** — Lotes vencidos ficam **bloqueados** para saída por requisição/consumo; só podem sair como descarte/perda ou ajuste.
- **RN09** — O custo do estoque é calculado por **custo médio ponderado** (por item e almoxarifado).
- **RN10** — Transferência entre almoxarifados gera saída na origem e entrada no destino de forma **atômica**, preservando lote/série.
- **RN11** — Ajustes de inventário exigem justificativa e perfil autorizado.
- **RN12** — Itens inativos não podem ser movimentados, mas mantêm o histórico.
- **RN13** — Os dados de uma organização são **isolados** e nunca visíveis a outra.

## 7. Requisitos não funcionais

- **Usabilidade:** API e mensagens em português; pensada para um front-end simples, utilizável por pessoas sem conhecimento técnico.
- **Segurança:** autenticação obrigatória (ex.: JWT); controle de acesso por perfil (RBAC); senhas com hash; isolamento de dados por organização.
- **Confiabilidade:** movimentações transacionais, com controle de concorrência para impedir saldos inconsistentes; backup diário.
- **Desempenho:** consultas de saldo e buscas de itens em < 1 s para até ~50 mil itens por organização.
- **Rastreabilidade:** log de auditoria imutável de ações sensíveis.
- **Configurabilidade:** comportamento ajustável por organização (ex.: antecedência do alerta de validade, regras de saída) sem alterar código.
- **Documentação:** API REST documentada com **OpenAPI/Swagger**.
- **Manutenibilidade:** código organizado em **Clean Architecture** + **DDD**, domínio independente de framework, com testes automatizados.

## 8. Diretrizes técnicas

| Aspecto | Decisão |
|---------|---------|
| Linguagem / Framework | Java + **Spring Boot** |
| Banco de dados | **PostgreSQL** |
| Documentação da API | **Swagger / OpenAPI** (springdoc-openapi) |
| Estilo arquitetural | **Monólito modular** com **Spring Modulith**; módulos se comunicam **por eventos** |
| Arquitetura interna dos módulos | **Clean Architecture** |
| Modelagem | **DDD** (Domain-Driven Design) |
| Estilo de API | REST |

### 8.1 Princípios de arquitetura

- **Domínio no centro:** entidades, agregados, objetos de valor e regras de negócio sem dependência de Spring, JPA ou banco.
- **Casos de uso** (camada de aplicação) orquestram o domínio e dependem apenas de **portas** (interfaces).
- **Adaptadores** (infraestrutura) implementam as portas: persistência (JPA/PostgreSQL), web (controllers REST), segurança etc.
- **Regra de dependência:** as dependências sempre apontam para dentro (infraestrutura → aplicação → domínio).
- **Linguagem ubíqua** em português, alinhada a este documento (Item, Lote, Almoxarifado, Movimentação, Requisição…).

### 8.2 Contextos delimitados candidatos (bounded contexts)

| Contexto | Responsabilidade |
|----------|------------------|
| **Organização & Acesso** | Organizações, usuários, perfis, permissões |
| **Catálogo** | Itens, categorias, unidades de medida, fornecedores, tipo de rastreabilidade |
| **Estrutura Física** | Almoxarifados e locais de armazenagem |
| **Estoque** | Saldos, movimentações, lotes, números de série, custo médio (núcleo do domínio) |
| **Requisições** | Pedidos de material e seu ciclo de vida |
| **Inventário** | Contagens, divergências e ajustes |
| **Relatórios & Alertas** | Consultas, indicadores e notificações |

> A definição final dos contextos, agregados e do modelo de dados ficará em `docs/architecture.md` e `docs/domain-model.md`.

## 9. Critérios de sucesso do MVP

- [ ] Uma organização pode ser configurada com um ou mais almoxarifados e seus locais.
- [ ] Almoxarife consegue cadastrar um item e registrar entrada e saída em menos de 1 minuto.
- [ ] Entradas de itens rastreados exigem lote/validade/série conforme configurado.
- [ ] A saída de itens com validade sugere automaticamente o lote pelo critério FEFO.
- [ ] É possível rastrear um lote ou número de série do recebimento até a saída.
- [ ] O saldo exibido bate com o kardex em 100% dos casos, inclusive com movimentações simultâneas.
- [ ] Alertas de estoque mínimo e de validade aparecem conforme configurado.
- [ ] Um inventário completo pode ser executado e ajustado pelo sistema.
- [ ] Todos os endpoints estão documentados no Swagger.

## 10. Evolução futura (roadmap indicativo)

1. **Fluxo de aprovação de requisições** (por valor, item, setor ou combinação)
2. Leitura de **código de barras / QR Code** e etiquetas
3. Dashboard com indicadores (giro de estoque, curva ABC, cobertura)
4. Módulo de compras integrado (sugestão automática de reposição)
5. Integrações (ERP, importação de XML de NF-e)
6. Controle de EPIs com ficha por colaborador
7. Controle de patrimônio

## 11. Premissas e restrições

- O sistema deve servir a **empresas de segmentos variados**; regras específicas de um segmento devem ser **configuráveis**, não fixas no código.
- O número de almoxarifados varia por organização (de 1 a vários).
- Não há integração com sistemas existentes no MVP.
- Back-end exposto como **API REST**; o front-end será tratado separadamente.

## 12. Perguntas em aberto

- [ ] **Modelo de implantação:** uma única instalação atendendo várias empresas (SaaS multi-tenant) ou uma instalação separada para cada empresa? *(Este documento assume multi-tenant com isolamento de dados — RN13.)*
- [ ] **Aprovação de requisições:** critérios a definir (adiado).
- [ ] **Front-end:** qual tecnologia (ex.: Angular, React) e se faz parte deste repositório.
- [ ] **Versão do Java / Spring Boot** (sugestão: Java 21 + Spring Boot 3.x).
- [ ] **Hospedagem:** nuvem, servidor próprio ou contêineres (Docker)?
- [ ] **Unidades de medida e conversão:** um item pode ser comprado em uma unidade (ex.: caixa) e consumido em outra (ex.: unidade)?
