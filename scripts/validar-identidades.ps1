param([string]$ApiUrl='http://127.0.0.1:8080',[string]$KeycloakUrl='http://127.0.0.1:8181')
$ErrorActionPreference='Stop'
# Exclusivo para o realm local: senha temporaria definida diretamente no Keycloak.
function Token($Realm,$Client,$Usuario,$Senha) {
    (Invoke-RestMethod -Method Post -Uri "$KeycloakUrl/realms/$Realm/protocol/openid-connect/token" -ContentType 'application/x-www-form-urlencoded' -Body @{grant_type='password';client_id=$Client;username=$Usuario;password=$Senha}).access_token
}
$access=Token 'almoxarifado' 'almoxarifado-frontend' 'admin.plataforma' 'Admin@123'
$headers=@{Authorization="Bearer $access"}
function Api($Metodo,$Rota,$Body=$null,$Chave=$null) {
    $h=@{Authorization=$headers.Authorization};if($Chave){$h['Idempotency-Key']=$Chave}
    $p=@{Method=$Metodo;Uri="$ApiUrl/api/v1$Rota";Headers=$h;ContentType='application/json';TimeoutSec=15}
    if($null -ne $Body){$p.Body=$Body|ConvertTo-Json -Depth 10 -Compress}
    Invoke-RestMethod @p
}
function Concluir($Pedido) {
    $fim=[DateTime]::UtcNow.AddSeconds(30)
    do {
        $r=Api 'Get' "/usuarios/provisionamentos/$($Pedido.id)"
        if($r.status -eq 'CONCLUIDO'){return $r}
        if($r.status -in @('FALHA','SUPERADO')){throw "Provisionamento: $($r.status), $($r.erro)"}
        Start-Sleep -Milliseconds 250
    } while([DateTime]::UtcNow -lt $fim)
    throw "Provisionamento nao concluiu: $($r.status)"
}
$username='validacao.'+[Guid]::NewGuid().ToString('N').Substring(0,12)
$conta=@{username=$username;nome='Validacao Identidade';email="$username@example.invalid";perfil='ALMOXARIFE';ativo=$true;almoxarifados=@()}
$chave=[Guid]::NewGuid().ToString()
$job=Concluir (Api 'Post' '/usuarios/provisionamentos' $conta $chave)
$repetido=Api 'Post' '/usuarios/provisionamentos' $conta $chave
if($repetido.id -ne $job.id){throw 'Repeticao nao idempotente.'}
$adminUser=if($env:KEYCLOAK_ADMIN){$env:KEYCLOAK_ADMIN}else{'admin'}
$adminPassword=if($env:KEYCLOAK_ADMIN_PASSWORD){$env:KEYCLOAK_ADMIN_PASSWORD}else{'admin'}
$admin=Token 'master' 'admin-cli' $adminUser $adminPassword
$ah=@{Authorization="Bearer $admin"}
$uri="$KeycloakUrl/admin/realms/almoxarifado/users/$($job.keycloakId)"
try {
    $senha=[Guid]::NewGuid().ToString()+'aA!9'
    $null=Invoke-RestMethod -Method Put -Uri "$uri/reset-password" -Headers $ah -ContentType 'application/json' -Body (@{type='password';value=$senha;temporary=$false}|ConvertTo-Json)
    $null=Invoke-RestMethod -Method Put -Uri $uri -Headers $ah -ContentType 'application/json' -Body '{"requiredActions":[],"emailVerified":true}'
    $scoped=Token 'almoxarifado' 'almoxarifado-frontend' $username $senha
    $part=$scoped.Split('.')[1].Replace('-','+').Replace('_','/');while($part.Length%4){$part+='='}
    $claims=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($part))|ConvertFrom-Json
    if($claims.escopo_restrito -ne $true -or $claims.realm_access.roles -notcontains 'ALMOXARIFE'){throw 'Claims de perfil/escopo invalidos.'}
    $locais=Invoke-RestMethod -Uri "$ApiUrl/api/v1/almoxarifados" -Headers @{Authorization="Bearer $scoped"}
    if($locais.Count -ne 0){throw 'Escopo vazio expos almoxarifados.'}
    Write-Output 'OK: conta real, role, JWT com escopo vazio restrito e repeticao idempotente.'
} finally {
    $conta.ativo=$false
    $null=Concluir (Api 'Put' "/usuarios/$($job.keycloakId)/identidade" $conta ([Guid]::NewGuid().ToString()))
    $remoto=Invoke-RestMethod -Uri $uri -Headers $ah
    if($remoto.enabled){throw 'Conta de validacao permaneceu ativa.'}
    Write-Output "OK: desativacao sincronizada; conta de validacao preservada sem acesso: $username"
}
