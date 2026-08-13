$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

Write-Host '=== Budget AI - NVIDIA NIM ===' -ForegroundColor Cyan

if (-not $env:NVIDIA_API_KEY) {
  $secure = Read-Host 'Cole sua NVIDIA API Key (nao sera salva em arquivo)' -AsSecureString
  $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
  try { $env:NVIDIA_API_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) }
  finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}

if (-not $env:NVIDIA_MODEL) { $env:NVIDIA_MODEL = 'z-ai/glm-5.2' }

if (-not $env:OPENAI_API_KEY) {
  Write-Host 'OPENAI_API_KEY nao configurada: comandos por texto funcionam via NVIDIA NIM; transcricao de audio ficara indisponivel.' -ForegroundColor Yellow
}

$java = Join-Path $root 'runtime\bin\java.exe'
$jar = Join-Path $root 'budget-ai.jar'
if (-not (Test-Path $java)) { throw "JRE embutido nao encontrado: $java" }
if (-not (Test-Path $jar)) { throw "Aplicacao nao encontrada: $jar" }

Write-Host "Modelo: $env:NVIDIA_MODEL" -ForegroundColor Green
Write-Host 'Painel: http://localhost:8080' -ForegroundColor Green
Start-Process 'http://localhost:8080'
& $java -jar $jar
