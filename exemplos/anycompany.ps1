# Cliente simples da API SOAP da AnyCompany (Windows PowerShell 5.1 ou PowerShell 7).
#
# Uso:
#   $env:ANYCOMPANY_API_KEY = "<api key do lojista, veja no backoffice em Lojistas>"
#   .\anycompany.ps1 autorizar <cartao> <valor> [captura: true|false]
#   .\anycompany.ps1 capturar  <transacaoId>
#   .\anycompany.ps1 cancelar  <transacaoId>
#   .\anycompany.ps1 consultar <transacaoId>
#
# Se o Windows bloquear a execucao do script:
#   powershell -ExecutionPolicy Bypass -File .\anycompany.ps1 autorizar 4111111111111111 150.00
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet("autorizar", "capturar", "cancelar", "consultar")]
    [string]$Operacao,

    [Parameter(Mandatory = $true, Position = 1)]
    [string]$Argumento,

    [Parameter(Position = 2)]
    [string]$Valor,

    [Parameter(Position = 3)]
    [ValidateSet("true", "false")]
    [string]$Captura = "false"
)

$ErrorActionPreference = "Stop"

if (-not $env:ANYCOMPANY_API_KEY) {
    throw "Defina `$env:ANYCOMPANY_API_KEY com a API key do lojista (backoffice > Lojistas)"
}
$url = if ($env:ANYCOMPANY_URL) { $env:ANYCOMPANY_URL } else { "http://127.0.0.1:8080/anycompany/services/PagamentoService" }

if ($Operacao -eq "autorizar") {
    if (-not $Valor) { throw "Uso: .\anycompany.ps1 autorizar <cartao> <valor> [true|false]" }
    $pedido = "PED-" + [DateTimeOffset]::Now.ToUnixTimeSeconds()
    $corpo = (Get-Content -Raw (Join-Path $PSScriptRoot "autorizar.xml")).
        Replace("{{API_KEY}}", $env:ANYCOMPANY_API_KEY).
        Replace("{{PEDIDO}}", $pedido).
        Replace("{{CARTAO}}", $Argumento).
        Replace("{{VALOR}}", $Valor).
        Replace("{{CAPTURA}}", $Captura)
} else {
    $corpo = (Get-Content -Raw (Join-Path $PSScriptRoot "operacao.xml")).
        Replace("{{OPERACAO}}", $Operacao).
        Replace("{{API_KEY}}", $env:ANYCOMPANY_API_KEY).
        Replace("{{TRANSACAO_ID}}", $Argumento)
}

$resposta = Invoke-WebRequest -UseBasicParsing -Method Post -Uri $url -ContentType "text/xml; charset=utf-8" -Body $corpo
[xml]$xml = $resposta.Content
$xml.GetElementsByTagName("resposta")[0].ChildNodes | ForEach-Object { "{0}: {1}" -f $_.Name, $_.InnerText }
