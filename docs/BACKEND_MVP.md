# Backend operacional do MVP

Todas as rotas abaixo usam o prefixo `/api/v1`. A autenticação continua no
Keycloak; não existem login, refresh ou armazenamento de senha no backend.
O Swagger em `/swagger-ui/index.html` apresenta os DTOs reais.

## Módulos e contratos implementados

| Módulo | Rotas principais | Comportamento |
|---|---|---|
| shared | `GET /auth/me` | Identidade, roles e organização do JWT; portas de auditoria e eventos |
| organizacao | `POST /organizacoes`, `GET/PUT /organizacao` | Criação por administrador da plataforma; consulta e alteração do tenant atual |
| organizacao | `GET/PUT /organizacao/configuracoes` | Prazo de alerta de validade entre 1 e 365 dias; padrão 30 |
| organizacao | `GET/PUT /setores`, `GET/PUT /usuarios` | Cadastros locais por código; inativação de usuário por `ativo=false` |
| catalogo | `GET/POST /catalogo/{categorias,unidades-medida,fornecedores,itens}` | Cadastros separados do saldo, com códigos únicos por tenant |
| catalogo | `GET/PUT /catalogo/{recurso}/{id}` | Consulta, alteração e inativação; rastreabilidade do item permanece imutável |
| estrutura | `GET/POST /almoxarifados`, `GET/PUT /almoxarifados/{id}` | Almoxarifados do tenant |
| estrutura | `GET/POST /almoxarifados/{id}/locais`, `PUT /almoxarifados/{id}/locais/{localId}` | Local vinculado a um almoxarifado do mesmo tenant |
| estoque | `POST /estoque/movimentacoes/{entradas,saidas,transferencias,ajustes,estornos}` | Operações transacionais, idempotentes e auditadas |
| estoque | `GET /estoque/{saldos,custos,movimentacoes,kardex}?itemId={uuid}` | Posições por local/lote/série, custo médio por almoxarifado e histórico |
| requisicao | `GET/POST /requisicoes`, `GET/PUT /requisicoes/{id}` | Rascunho com até 100 itens distintos; edição antes da reserva |
| requisicao | `POST /requisicoes/{id}/{enviar,separar,entregar,cancelar,confirmar-recebimento}` | Reserva, separação, entrega integral, liberação e confirmação |
| inventario | `GET/POST /inventarios`, `GET /inventarios/{id}` | Snapshot das posições existentes de um local |
| inventario | `PUT /inventarios/{id}/contagens/{posicaoId}`, `POST /inventarios/{id}/{finalizar,cancelar}` | Contagem, diferenças e ajuste com justificativa |
| alerta | `GET /alertas`, `POST /alertas/dispensar`, `GET /dashboard` | Mínimo disponível, validade próxima, vencimento e indicadores |

Listagens de catálogo, estrutura, requisições, inventários, alertas e histórico
aceitam `pagina` a partir de zero e `tamanho` entre 1 e 100. Os resultados são
arrays. Organização e listagem de saldos mantêm respostas específicas.

## Identidade e isolamento

- `organizacao_id` é obtido exclusivamente do JWT. O corpo de uma operação de
  negócio não escolhe o tenant. A exceção é a criação de uma organização por
  `ADMIN_PLATAFORMA`, que informa o UUID a provisionar no Keycloak.
- Todas as consultas e alterações dos novos adaptadores usam o tenant. As FKs
  internas compostas impedem relacionar cadastros de organizações diferentes.
- `codigo` de usuário é a identidade externa no Keycloak; o cadastro local não
  cria contas, troca senhas nem concede roles no provedor. Autorizações continuam
  usando as roles assinadas do JWT. Alterações de acesso devem ser feitas também
  no Keycloak, considerando a validade dos tokens já emitidos.
- Administradores e almoxarifes movimentam estoque. Requisitantes operam seus
  próprios pedidos; gestores consultam os dados operacionais do tenant.
- Nenhum cadastro de material, almoxarifado ou posição de estoque é criado
  automaticamente pela existência de um claim no token.

## Estoque e rastreabilidade

O modelo operacional usa `estoque.posicoes`, `custos`, `operacoes`,
`linhas_operacao` e `reservas`. As tabelas do protótipo continuam preservadas,
sem novos endpoints de escrita. Dados legados não são convertidos automaticamente,
pois não informam organização, item de catálogo e localização de destino.

Cada POST de movimentação exige um UUID no header `Idempotency-Key`. Repetir a
mesma chave e o mesmo comando devolve a operação existente sem novo efeito.
Reutilizar a chave com outro conteúdo é rejeitado. Na integração assíncrona,
as chaves são derivadas do comando e da linha, permitindo reprocessamento.

```json
{
  "itemId": "UUID do item",
  "destinoId": "UUID do local",
  "quantidade": 10,
  "custo": 2.5,
  "motivo": "Recebimento de compra"
}
```

- Na saída, informe `origemId`; na transferência, informe origem e destino.
- O item define `NENHUM`, `LOTE`, `LOTE_VALIDADE` ou `NUMERO_SERIE`.
  Entradas de série exigem uma unidade por chamada; a série é única por item e
  tenant enquanto está em estoque. Um lote não aceita validades conflitantes.
- Saídas selecionam FEFO quando existe validade e FIFO nos demais casos;
  `lote` ou `serie` podem restringir a seleção. Lotes vencidos não são elegíveis.
- Ajustes usam `destinoId`, quantidade com sinal e motivo obrigatório. Ajuste
  negativo pode registrar perda de material vencido. Não pode consumir reserva.
- Estornos informam `referenciaId` da operação original e o mesmo `itemId`.
  Só podem ocorrer uma vez, e falham se o saldo necessário não estiver disponível.
- Quantidade e reserva têm constraints de não negatividade. Escritas usam lock
  transacional por tenant/item; transferências atualizam as duas pontas e o
  custo médio na mesma transação. Não há transação distribuída entre módulos.
- Custo médio ponderado é mantido por item e almoxarifado. Transferências levam
  o custo da origem; a seleção física FEFO/FIFO não altera a regra contábil.
- Histórico e auditoria têm triggers que rejeitam UPDATE, DELETE e TRUNCATE.
  O kardex aceita filtros `lote` e `serie` e mostra as linhas com sinal e custo.
- As datas de validade são avaliadas em UTC neste MVP.

## Integração e consistência

Os módulos não chamam beans de negócio uns dos outros nem leem tabelas alheias.
Catálogo, estrutura e estoque mantêm projeções atualizadas por eventos de
organização e cadastros. Inventário e alertas mantêm projeções dos saldos.
Contratos de ajuste de inventário ficam em `shared.events` para evitar ciclos
de dependência. `ApplicationModules.verify()` valida esse grafo.

Os consumidores executam após commit, em outra thread e transação, com o Event
Publication Registry do Modulith. Projeções de saldo usam revisão crescente,
rejeitando eventos duplicados e antigos. Uma criação pode levar alguns instantes
para se tornar disponível no próximo módulo; o cliente deve consultar o estado
e repetir operações idempotentes quando receber indisponibilidade de projeção.

Requisições seguem:

```text
RASCUNHO -> RESERVANDO -> RESERVADA -> SEPARADA -> ENTREGANDO -> ENTREGUE -> FINALIZADA
                |                        |
          FALHA_RESERVA                CANCELANDO -> CANCELADA
```

A reserva de todas as linhas é atômica. Uma falha desfaz todas as reservas. Uma
falha de entrega mantém a requisição separada e as reservas ativas. Cancelar um
pedido reservado libera as reservas; cancelar um rascunho é imediato. Respostas
das ações retornam HTTP 202; o cliente consulta `GET /requisicoes/{id}` até o
resultado. Erros de negócio viram resultados explícitos; falhas de infraestrutura
deixam a publicação pendente para reprocessamento.

Inventários seguem `CONTAGEM -> FINALIZANDO -> CONCLUIDO` ou `CONFLITO`. Todas as
posições do snapshot precisam ser contadas. A revisão de cada posição é validada
antes de qualquer ajuste. Se houver movimento ou reserva durante a contagem,
nenhuma alteração parcial é aplicada; abra um novo inventário. Só um inventário
em andamento é permitido por local. Posições novas fora do snapshot não são
ajustadas por esse inventário.

Alertas são calculados sobre projeções locais a cada consulta. Isso permite
detectar a passagem da data de validade sem exigir nova movimentação. Material
vencido não conta como disponibilidade para o mínimo. Dispensas se referem à
revisão do alerta; uma mudança posterior pode gerar novo aviso. O dashboard
conta os avisos visíveis, excluindo as revisões dispensadas.

## Validação

```powershell
mvn -f backend/pom.xml clean verify
docker compose up -d --build --wait
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/validar-fundacao.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/validar-backend.ps1
```

O último script cria dados de demonstração identificados por códigos únicos e
preserva o histórico. Valida JWT real, cadastros, entrada repetida com a mesma
chave, transferência, pedido completo, inventário, alerta e dashboard. Não imprime
tokens. Deve ser usado no ambiente local com o realm de desenvolvimento.

Testes automatizados cobrem PostgreSQL, isolamento entre tenants, RBAC, saldo
concorrente, falha de gravação na transferência, FEFO/FIFO, série, custo, estorno,
reserva e entrega, inventário concorrente, alertas, auditoria e projeções fora de
ordem. As migrações originais continuam intactas; alterações são versionadas.

## Itens da especificação ainda além desta entrega

- Limites mínimo/máximo e permissões específicas por almoxarifado; atualmente o
  limite do catálogo é agregado por item/organização, e as roles valem no tenant.
- Atendimento parcial de pedidos, troca manual de lote de uma reserva e vínculo
  do pedido a centro de custo/setor; a entrega atual é integral.
- Inventário de materiais ainda sem posição cadastrada e contagem cega.
- Relatórios gerenciais de consumo por setor e exportação.
- Provisionamento e sincronização administrativa de contas/roles no Keycloak.
- Recuperação/backfill de novas projeções ao adicionar consumidores em uma
  instalação que já tenha histórico anterior desses eventos.
- Frontend operacional e integração OIDC da SPA, que não foram alterados aqui.
