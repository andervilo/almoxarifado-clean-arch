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
| requisicao | `POST /requisicoes/{id}/{enviar,separar,entregar,cancelar,confirmar-recebimento}` | Reserva, separação, entrega integral ou parcial, liberação e confirmação |
| inventario | `GET/POST /inventarios`, `GET /inventarios/{id}` | Snapshot do local, inclusive vazio; contagem cega opcional |
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
  cria contas nem concede roles no provedor. Para sincronizar contas e roles,
  use os endpoints de provisionamento descritos abaixo. Autorizações usam as
  roles assinadas do JWT, considerando a validade dos tokens já emitidos.
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
falha de entrega restaura o estado anterior e mantém as reservas ativas. Cancelar um
pedido reservado libera as reservas; cancelar um rascunho é imediato. Respostas
das ações retornam HTTP 202; o cliente consulta `GET /requisicoes/{id}` até o
resultado. Erros de negócio viram resultados explícitos; falhas de infraestrutura
deixam a publicação pendente para reprocessamento.

Inventários seguem `CONTAGEM -> FINALIZANDO -> CONCLUIDO` ou `CONFLITO`. Todas as
posições do snapshot precisam ser contadas. A revisão de cada posição é validada
antes de qualquer ajuste. Se houver movimento ou reserva durante a contagem,
nenhuma alteração parcial é aplicada; abra um novo inventário. Só um inventário
em andamento é permitido por local. Novas posições físicas podem ser informadas
na contagem; posições surgidas por movimentação concorrente causam conflito.

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
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/reconstruir-projecoes.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/validar-backend.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/validar-identidades.ps1
```

O último script cria dados de demonstração identificados por códigos únicos e
preserva o histórico. Valida JWT real, cadastros, entrada repetida com a mesma
chave, transferência, pedido completo, inventário, alerta e dashboard. Não imprime
tokens. Deve ser usado no ambiente local com o realm de desenvolvimento.

Testes automatizados cobrem PostgreSQL, isolamento entre tenants, RBAC, saldo
concorrente, falha de gravação na transferência, FEFO/FIFO, série, custo, estorno,
reserva e entrega, inventário concorrente, alertas, auditoria e projeções fora de
ordem. As migrações originais continuam intactas; alterações são versionadas.

## Escopo por almoxarifado e reposição

O JWT pode conter `almoxarifados` como array de UUIDs. Consultas filtram esse
escopo antes da paginação; escritas verificam todas as posições envolvidas.
Admins de organização e plataforma têm acesso aos almoxarifados do tenant.
Sem esses claims, permanece o acesso legado ao tenant inteiro. Com
`escopo_restrito=true`, a ausência da lista significa acesso a nenhum
almoxarifado. Atributos são editáveis apenas pela administração do Keycloak.

`GET/PUT /catalogo/itens/{id}/parametros-reposicao/{almoxarifadoId}` configura
`minimo` e `maximo` por item/almoxarifado. O mínimo compara disponibilidade sem
reservas ou vencidos; o máximo compara quantidade física. São alertas, sem
bloqueio de entradas. O mínimo global legado é usado quando não existem políticas
específicas, e é ocultado de leitores com escopo restrito para evitar vazamento
de saldos agregados. Alertas incluem `almoxarifadoId`; dashboard inclui máximo.

## Entrega parcial, alocações e consumo

`POST /requisicoes/{id}/entregar` sem corpo entrega o restante. Para entregar
parte, envie `{"linhas":[{"itemId":"UUID","quantidade":2}]}` e um UUID em
`Idempotency-Key`. Repetir a mesma chave/corpo não duplica a entrega; outro
conteúdo é rejeitado. Linhas retornam `atendida` e `pendente`; o estado passa a
`PARCIALMENTE_ENTREGUE`. Cancelar libera apenas o restante reservado.

`GET /requisicoes/{id}/alocacoes` mostra posições, lotes e séries reservados.
`PUT /requisicoes/{id}/alocacoes/{itemId}` recebe `motivo` e
`alocacoes:[{posicaoId,quantidade}]`, com `Idempotency-Key`. O total deve equivaler
ao pendente. A troca é atômica dentro do mesmo item/local; indisponibilidade ou
lote vencido preservam a reserva anterior. O estado transitório é `REALOCANDO`.

Setores aceitam `centroCusto`; pedidos aceitam `setorCodigo`. O pedido preserva
o centro histórico. `GET /relatorios/consumo-por-setor?inicio=AAAA-MM-DD&fim=AAAA-MM-DD`
retorna consumo líquido por setor/centro/item, usando data UTC e custo efetivo
da entrega, incluindo estornos. O período máximo é 366 dias. A variante
`/relatorios/consumo-por-setor/exportar` gera CSV UTF-8 com BOM, proteção contra
fórmulas, limite explícito de 10.000 linhas e auditoria da exportação.

## Inventário cego e novas posições

Na abertura, `contagemCega=true` oculta `sistema` e `diferenca` até conclusão ou
cancelamento. Essa apresentação não altera as permissões de consulta de estoque.
`POST /inventarios/{id}/contagens` recebe `itemId`, `lote`, `serie`, `validade`,
`quantidade` positiva e `custo` não negativo para material sem posição anterior.
O UUID da contagem será o da nova posição. São permitidas até 1.000 posições por
inventário. A finalização bloqueia o local, confere o conjunto completo de
posições e suas revisões e aplica todos os ajustes na mesma transação. Qualquer
conflito desfaz tudo, inclusive posições novas.

## Provisionamento administrativo de identidade

Admins podem chamar `POST /usuarios/provisionamentos` ou
`PUT /usuarios/{keycloakId}/identidade`, com UUID em `Idempotency-Key` e corpo
`{username,nome,email,perfil,ativo,almoxarifados}`. Lista nula libera o tenant;
lista vazia restringe todos os almoxarifados. `ADMIN_PLATAFORMA` e contas
administrativas do Keycloak são protegidas e não podem ser provisionadas aqui.

Consulte `GET /usuarios/provisionamentos/{id}`. O fluxo durável é
`PENDENTE -> ATIVANDO -> CONCLUIDO`: a conta fica desabilitada enquanto roles,
atributos e cadastro local são gravados. Erros de negócio ficam em `FALHA`;
falhas de infraestrutura preservam eventos para retry. A ação POST
`/usuarios/provisionamentos/{id}/reprocessar` permite retomada. Pedidos antigos
substituídos por configuração posterior tornam-se `SUPERADO` e não reativam a
conta. Alterações revogam sessões; JWTs emitidos seguem válidos até expirar.

O backend não recebe senhas nem envia e-mails. Novas contas exigem atualização
de senha no Keycloak; o onboarding é administrado no provedor. Veja
[keycloak-users.md](keycloak-users.md) para configuração e segredos.

## Reconstrução das projeções

Em instalações existentes, execute `scripts/reconstruir-projecoes.ps1` após
atualizar o backend. O script aceita `ALMOX_ACCESS_TOKEN`; sem ele, usa a conta
local de demonstração. Publica snapshots paginados dos cadastros e histórico,
sem alterar quantidades físicas ou lançamentos imutáveis. A ação é auditada.

Endpoints administrativos POST `/{modulo}/sincronizacoes/{recurso}` aceitam
`apos=UUID` e `tamanho=1..100`. Percorra `proximoCursor` até `concluido=true`.
Recursos: organização (`organizacao`, `setores`), estrutura (`almoxarifados`,
`locais`), catálogo (`itens`, `parametros-reposicao`) e estoque (`saldos`,
`reservas`, `entregas`). Os consumidores atualizam projeções após commit.

## Validação desta evolução e próximo escopo

`mvn clean verify`: 86 testes, sem falhas, incluindo PostgreSQL, fronteiras
Modulith/ArchUnit, entregas parciais, realocação, escopos, inventário cego,
republicação e provisionamento. Docker Compose saudável e scripts de fundação,
workflow, reconstrução e identidade validados com Keycloak real.

O frontend operacional e a integração OIDC da SPA continuam pendentes, assim
como sua validação de ponta a ponta. Esta entrega cobre o backend.
