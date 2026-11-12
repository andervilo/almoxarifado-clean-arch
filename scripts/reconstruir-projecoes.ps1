param([string]$ApiUrl='http://127.0.0.1:8080',[string]$KeycloakUrl='http://127.0.0.1:8181')
$ErrorActionPreference='Stop'
$accessToken=$env:ALMOX_ACCESS_TOKEN
if([string]::IsNullOrWhiteSpace($accessToken)) {
    # Credenciais do realm de desenvolvimento; um token administrativo pode ser fornecido pelo ambiente.
    $resposta=Invoke-RestMethod -Method Post -Uri "$KeycloakUrl/realms/almoxarifado/protocol/openid-connect/token" `
        -ContentType 'application/x-www-form-urlencoded' -Body @{grant_type='password';client_id='almoxarifado-frontend';username='admin.plataforma';password='Admin@123'}
    $accessToken=$resposta.access_token
}
$headers=@{Authorization="Bearer $accessToken"}
$rotas=@('organizacao/sincronizacoes/organizacao','organizacao/sincronizacoes/setores','estrutura/sincronizacoes/almoxarifados',
    'estrutura/sincronizacoes/locais','catalogo/sincronizacoes/itens','catalogo/sincronizacoes/parametros-reposicao','estoque/sincronizacoes/saldos','estoque/sincronizacoes/reservas','estoque/sincronizacoes/entregas')
foreach($rota in $rotas) {
    $cursor=$null;$registros=0;$paginas=0
    do {
        $query='?tamanho=100';if($cursor){$query+="&apos=$cursor"}
        $lote=Invoke-RestMethod -Method Post -Uri "$ApiUrl/api/v1/$rota$query" -Headers $headers -TimeoutSec 30
        $registros+=$lote.registros;$paginas++
        if(-not $lote.concluido -and $lote.proximoCursor -eq $cursor){throw 'Paginacao sem progresso.'}
        $cursor=$lote.proximoCursor
        if($paginas -gt 10000){throw 'Limite de paginas atingido; continue manualmente pelo cursor.'}
    } while(-not $lote.concluido)
    Write-Output "OK: $rota - $registros registros publicados em $paginas pagina(s)"
}
Write-Output 'Snapshots publicados. Os consumidores atualizam as projecoes apos commit.'
