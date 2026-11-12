param([string]$ApiUrl='http://127.0.0.1:8080',[string]$KeycloakUrl='http://127.0.0.1:8181')
$ErrorActionPreference='Stop'
$token=Invoke-RestMethod -Method Post -Uri "$KeycloakUrl/realms/almoxarifado/protocol/openid-connect/token" `
    -ContentType 'application/x-www-form-urlencoded' -Body @{grant_type='password';client_id='almoxarifado-frontend';username='admin.plataforma';password='Admin@123'}
$headers=@{Authorization="Bearer $($token.access_token)"}

function Api([string]$Metodo,[string]$Rota,$Body=$null,[hashtable]$Extra=@{}) {
    $cabecalhos=@{};foreach($chave in $headers.Keys){$cabecalhos[$chave]=$headers[$chave]}
    foreach($chave in $Extra.Keys){$cabecalhos[$chave]=$Extra[$chave]}
    $parametros=@{Method=$Metodo;Uri="$ApiUrl/api/v1$Rota";Headers=$cabecalhos;TimeoutSec=15}
    if($null -ne $Body){$parametros.Body=($Body|ConvertTo-Json -Depth 10 -Compress);$parametros.ContentType='application/json'}
    return Invoke-RestMethod @parametros
}
function Aguardar([scriptblock]$Acao,[scriptblock]$Condicao) {
    $fim=[DateTime]::UtcNow.AddSeconds(30);$ultimoErro=$null
    do {
        try {$resposta=& $Acao;if(& $Condicao $resposta){return $resposta}} catch {$ultimoErro=$_.Exception.Message}
        Start-Sleep -Milliseconds 250
    } while([DateTime]::UtcNow -lt $fim)
    throw "Fluxo nao concluiu no prazo. $ultimoErro"
}

$org='0190f5a0-0000-7000-8000-000000000001'
try {$null=Api 'Get' '/organizacao'} catch {
    if([int]$_.Exception.Response.StatusCode -ne 404){throw}
    $null=Api 'Post' '/organizacoes' @{id=$org;nome='Organizacao demonstracao';codigo='DEMO'}
}
$sufixo=[Guid]::NewGuid().ToString('N').Substring(0,10)
$categoria=Aguardar {Api 'Post' '/catalogo/categorias' @{codigo="C-$sufixo";nome='Categoria teste';ativo=$true}} {param($r) $null -ne $r.id}
$unidade=Api 'Post' '/catalogo/unidades-medida' @{codigo="U-$sufixo";nome='Unidade teste';ativo=$true}
$item=Api 'Post' '/catalogo/itens' @{codigo="I-$sufixo";descricao='Material teste';categoriaId=$categoria.id;unidadeMedidaId=$unidade.id;rastreabilidade='NENHUM';estoqueMinimo=50;estoqueMaximo=100;ativo=$true}
$almox=Api 'Post' '/almoxarifados' @{codigo="A-$sufixo";nome='Almoxarifado teste';ativo=$true}
$origem=Api 'Post' "/almoxarifados/$($almox.id)/locais" @{codigo='ORIGEM';descricao='Prateleira origem';ativo=$true}
$destino=Api 'Post' "/almoxarifados/$($almox.id)/locais" @{codigo='DESTINO';descricao='Prateleira destino';ativo=$true}
$entrada=@{itemId=$item.id;destinoId=$origem.id;quantidade=10;custo=2;motivo='Validacao real do backend'}
$idempotencia=@{'Idempotency-Key'=[Guid]::NewGuid().ToString()}
$null=Aguardar {Api 'Post' '/estoque/movimentacoes/entradas' $entrada $idempotencia} {param($r) $null -ne $r.id}
$null=Api 'Post' '/estoque/movimentacoes/entradas' $entrada $idempotencia
$transferenciaIdempotencia=@{'Idempotency-Key'=[Guid]::NewGuid().ToString()}
$null=Aguardar {Api 'Post' '/estoque/movimentacoes/transferencias' @{itemId=$item.id;origemId=$origem.id;destinoId=$destino.id;quantidade=2;motivo='Transferencia de teste'} $transferenciaIdempotencia} {param($r) $null -ne $r.id}
$req=Api 'Post' '/requisicoes' @{localId=$origem.id;linhas=@(@{itemId=$item.id;quantidade=4})}
$null=Api 'Post' "/requisicoes/$($req.id)/enviar"
$null=Aguardar {Api 'Get' "/requisicoes/$($req.id)"} {param($r) $r.status -eq 'RESERVADA'}
$null=Api 'Post' "/requisicoes/$($req.id)/separar"
$null=Api 'Post' "/requisicoes/$($req.id)/entregar"
$null=Aguardar {Api 'Get' "/requisicoes/$($req.id)"} {param($r) $r.status -eq 'ENTREGUE'}
$null=Api 'Post' "/requisicoes/$($req.id)/confirmar-recebimento"
$null=Aguardar {Api 'Get' "/estoque/saldos?itemId=$($item.id)"} {param($r) ($r | Where-Object {$_.localId -eq $origem.id}).quantidade -eq 4}
# A projecao do inventario acompanha o estoque assincronamente.
$inventario=Aguardar {
    $novo=Api 'Post' '/inventarios' @{localId=$origem.id}
    if($novo.contagens[0].sistema -ne 4){$null=Api 'Post' "/inventarios/$($novo.id)/cancelar";throw 'Aguardando projecao do saldo.'}
    return $novo
} {param($r) $null -ne $r.id}
$posicao=$inventario.contagens[0]
if($posicao.sistema -ne 4){throw 'Projecao de inventario ainda desatualizada; repita a validacao.'}
$null=Api 'Put' "/inventarios/$($inventario.id)/contagens/$($posicao.posicaoId)" @{quantidade=3}
$null=Api 'Post' "/inventarios/$($inventario.id)/finalizar" @{justificativa='Divergencia fisica validada no teste'}
$null=Aguardar {Api 'Get' "/inventarios/$($inventario.id)"} {param($r) $r.status -eq 'CONCLUIDO'}
$saldos=Api 'Get' "/estoque/saldos?itemId=$($item.id)"
$total=($saldos|Measure-Object -Property quantidade -Sum).Sum
if($total -ne 5){throw "Saldo final esperado 5, recebido $total"}
$alertas=Aguardar {Api 'Get' '/alertas?tamanho=100'} {param($r) @($r|Where-Object {$_.itemId -eq $item.id -and $_.tipo -eq 'ESTOQUE_MINIMO'}).Count -eq 1}
$alerta=@($alertas|Where-Object {$_.itemId -eq $item.id})[0]
$null=Api 'Post' '/alertas/dispensar' @{chave=$alerta.chave;revisao=$alerta.revisao}
$null=Api 'Get' '/dashboard'
Write-Output "OK: catalogo, estrutura, entrada idempotente, transferencia, requisicao, inventario, alertas e dashboard. Saldo final: $total"
Write-Output "Item de validacao preservado: $($item.id)"
