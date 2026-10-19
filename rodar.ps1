# Compila e executa o TP1 com acentuação correta no console do Windows.
# Exemplos:
#   .\rodar.ps1 barbearia correto 12
#   .\rodar.ps1 barbearia sem-mutex 12
#   .\rodar.ps1 barbearia mutex-preso 12
#   .\rodar.ps1 fumantes correto 10
#   .\rodar.ps1 fumantes ingenuo 10
#   .\rodar.ps1 filosofos garcom 3
#   .\rodar.ps1 filosofos ingenuo 3
#   .\rodar.ps1 testes
param(
    [Parameter(Mandatory = $true)][ValidateSet('barbearia', 'fumantes', 'filosofos', 'testes')][string]$problema,
    [string]$modo = '',
    [string]$quantidade = ''
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Set-Location $PSScriptRoot

javac -encoding UTF-8 -d out src/util/*.java src/barbearia/*.java src/fumantes/*.java src/filosofos/*.java
if (-not $?) { exit 1 }

$java = @('-Dstdout.encoding=UTF-8', '-cp', 'out')
$argumentos = @($modo, $quantidade) | Where-Object { $_ -ne '' }

switch ($problema) {
    'barbearia' { java @java barbearia.MainBarbearia @argumentos }
    'fumantes'  { java @java fumantes.MainFumantes @argumentos }
    'filosofos' { java @java filosofos.MainFilosofos @argumentos }
    'testes'    { java @java barbearia.TesteBarbearia; java @java fumantes.TesteFumantes; java @java filosofos.TesteFilosofos }
}
