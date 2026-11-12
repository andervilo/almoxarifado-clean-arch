param(
    # IPv4 evita diferenças de resolução de localhost entre Windows e Docker Linux.
    [string]$ApiUrl = 'http://127.0.0.1:8080',
    [string]$KeycloakUrl = 'http://127.0.0.1:8181',
    [int]$TimeoutSegundos = 180
)

$ErrorActionPreference = 'Stop'

function Obter-StatusHttp([string]$Uri, [hashtable]$Headers = @{}) {
    try {
        return [int](Invoke-WebRequest -Uri $Uri -Headers $Headers -UseBasicParsing -TimeoutSec 5).StatusCode
    } catch {
        if ($null -ne $_.Exception.Response) {
            return [int]$_.Exception.Response.StatusCode
        }
        throw
    }
}

function Exigir-Status([int]$Esperado, [string]$Uri, [hashtable]$Headers = @{}) {
    $recebido = Obter-StatusHttp $Uri $Headers
    if ($recebido -ne $Esperado) {
        throw "HTTP inesperado em ${Uri}: esperado $Esperado, recebido $recebido"
    }
}

function Obter-Token([string]$ClientId, [string]$Usuario, [string]$Senha) {
    $resposta = Invoke-RestMethod -Method Post -TimeoutSec 15 `
        -Uri "$KeycloakUrl/realms/almoxarifado/protocol/openid-connect/token" `
        -ContentType 'application/x-www-form-urlencoded' `
        -Body @{ grant_type = 'password'; client_id = $ClientId; username = $Usuario; password = $Senha }
    return $resposta.access_token
}

$limite = [DateTime]::UtcNow.AddSeconds($TimeoutSegundos)
do {
    $pronto = $false
    try { $pronto = (Obter-StatusHttp "$ApiUrl/actuator/health/readiness") -eq 200 } catch { }
    if ($pronto) { break }
    Start-Sleep -Seconds 2
} while ([DateTime]::UtcNow -lt $limite)
if (-not $pronto) { throw 'A API nao ficou pronta dentro do prazo.' }

Exigir-Status 200 "$ApiUrl/actuator/health/liveness"
Exigir-Status 200 "$ApiUrl/v3/api-docs"
Exigir-Status 401 "$ApiUrl/api/v1/auth/me"
Exigir-Status 401 "$ApiUrl/api/v1/auth/me" @{ Authorization = 'Bearer invalido' }

# Credenciais exclusivamente do realm de desenvolvimento versionado neste repositorio.
$usuarios = @(
    @{ Login = 'admin.plataforma'; Senha = 'Admin@123'; Perfil = 'ADMIN_PLATAFORMA' },
    @{ Login = 'admin.organizacao'; Senha = 'OrgAdmin@123'; Perfil = 'ADMIN_ORGANIZACAO' },
    @{ Login = 'almoxarife'; Senha = 'Almoxarife@123'; Perfil = 'ALMOXARIFE' },
    @{ Login = 'requisitante'; Senha = 'Requisitante@123'; Perfil = 'REQUISITANTE' },
    @{ Login = 'gestor'; Senha = 'Gestor@123'; Perfil = 'GESTOR' }
)
foreach ($usuario in $usuarios) {
    $token = Obter-Token 'almoxarifado-frontend' $usuario.Login $usuario.Senha
    $headers = @{ Authorization = "Bearer $token" }
    $sessao = Invoke-RestMethod -Uri "$ApiUrl/api/v1/auth/me" -Headers $headers -TimeoutSec 10
    if ($sessao.nomeUsuario -ne $usuario.Login -or $sessao.perfis -notcontains $usuario.Perfil `
        -or $sessao.organizacaoId -ne '0190f5a0-0000-7000-8000-000000000001') {
        throw "Sessao incorreta para $($usuario.Login)"
    }
    $statusModulith = if ($usuario.Perfil -eq 'ADMIN_PLATAFORMA') { 200 } else { 403 }
    Exigir-Status $statusModulith "$ApiUrl/actuator/modulith" $headers
    Write-Output "OK: $($usuario.Perfil), organizacao e permissao do Actuator"
}

# Mesmo issuer e assinatura, mas token de outro client, sem a audience da API.
$outraAudience = Obter-Token 'admin-cli' 'admin.organizacao' 'OrgAdmin@123'
Exigir-Status 401 "$ApiUrl/api/v1/auth/me" @{ Authorization = "Bearer $outraAudience" }
Write-Output 'OK: saude, OpenAPI, cinco perfis, rejeicao de token e de audience incorreta'
