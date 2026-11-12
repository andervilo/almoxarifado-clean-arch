param([string]$KeycloakUrl='http://127.0.0.1:8181')
$ErrorActionPreference='Stop'
$adminUser=if($env:KEYCLOAK_ADMIN){$env:KEYCLOAK_ADMIN}else{'admin'}
$adminPassword=if($env:KEYCLOAK_ADMIN_PASSWORD){$env:KEYCLOAK_ADMIN_PASSWORD}else{'admin'}
$clientSecret=if($env:KEYCLOAK_ADMIN_CLIENT_SECRET){$env:KEYCLOAK_ADMIN_CLIENT_SECRET}else{'almox-local-admin-secret'}
$token=Invoke-RestMethod -Method Post -Uri "$KeycloakUrl/realms/master/protocol/openid-connect/token" -ContentType 'application/x-www-form-urlencoded' -Body @{grant_type='password';client_id='admin-cli';username=$adminUser;password=$adminPassword}
$headers=@{Authorization="Bearer $($token.access_token)"}
function Admin([string]$Metodo,[string]$Rota,$Body=$null) {
    $parametros=@{Method=$Metodo;Uri="$KeycloakUrl/admin/realms/almoxarifado$Rota";Headers=$headers;TimeoutSec=15}
    if($null -ne $Body){$parametros.Body=($Body|ConvertTo-Json -Depth 30 -Compress);$parametros.ContentType='application/json; charset=utf-8'}
    try {
        $resposta=Invoke-RestMethod @parametros
        if($resposta -is [System.Array]) {return $resposta | ForEach-Object {$_}}
        return $resposta
    } catch {throw "Falha administrativa: $Metodo $Rota (HTTP $([int]$_.Exception.Response.StatusCode))"}
}
$realm=Get-Content -LiteralPath "$PSScriptRoot/../docker/keycloak/import/almoxarifado-realm.json" -Raw -Encoding UTF8|ConvertFrom-Json
$client=@(Admin 'Get' '/clients?clientId=almoxarifado-admin')
if($client.Count -eq 0) {
    $config=$realm.clients|Where-Object {$_.clientId -eq 'almoxarifado-admin'};$config.secret=$clientSecret
    $null=Admin 'Post' '/clients' $config;$client=@(Admin 'Get' '/clients?clientId=almoxarifado-admin')
} else {
    $existente=Admin 'Get' "/clients/$($client[0].id)/client-secret"
    if($existente.value -ne $clientSecret){throw 'O client ja existe com outro segredo. Configure KEYCLOAK_ADMIN_CLIENT_SECRET com o segredo existente; nenhuma rotacao foi feita.'}
}
$id=$client[0].id;$servico=Admin 'Get' "/clients/$id/service-account-user"
$gerenciamento=@(Admin 'Get' '/clients?clientId=realm-management')[0]
$papeis=@('manage-users','view-users','view-realm'|ForEach-Object {Admin 'Get' "/clients/$($gerenciamento.id)/roles/$_"})
$null=Admin 'Post' "/users/$($servico.id)/role-mappings/clients/$($gerenciamento.id)" $papeis
$front=@(Admin 'Get' '/clients?clientId=almoxarifado-frontend')[0]
$mappers=@(Admin 'Get' "/clients/$($front.id)/protocol-mappers/models")
foreach($mapper in @($realm.clients|Where-Object {$_.clientId -eq 'almoxarifado-frontend'})[0].protocolMappers|Where-Object {$_.name -in @('almoxarifados','escopo_restrito')}) {
    $atual=@($mappers|Where-Object {$_.name -eq $mapper.name})
    if($atual.Count -eq 0){$null=Admin 'Post' "/clients/$($front.id)/protocol-mappers/models" $mapper}
    else {$mapper|Add-Member -NotePropertyName id -NotePropertyValue $atual[0].id -Force;$null=Admin 'Put' "/clients/$($front.id)/protocol-mappers/models/$($atual[0].id)" $mapper}
}
$perfil=Admin 'Get' '/users/profile'
$configPerfil=$realm.components.'org.keycloak.userprofile.UserProfileProvider'[0].config.'kc.user.profile.config'[0]|ConvertFrom-Json
foreach($nome in @('organizacao_id','almoxarifados','escopo_restrito','provisionamento_id')) {
    $attr=@($configPerfil.attributes|Where-Object {$_.name -eq $nome})[0]
    $perfil.attributes=@($perfil.attributes|Where-Object {$_.name -ne $nome})+@($attr)
}
$null=Admin 'Put' '/users/profile' $perfil
Write-Output 'OK: service account, permissoes administrativas limitadas, mappers de escopo e atributos protegidos configurados.'
