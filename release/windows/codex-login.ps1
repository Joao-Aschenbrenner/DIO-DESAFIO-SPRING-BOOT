$ErrorActionPreference = 'Stop'
Write-Host '=== Codex CLI - Login com ChatGPT ===' -ForegroundColor Cyan

$codex = Get-Command codex -ErrorAction SilentlyContinue
if (-not $codex) {
  Write-Host 'Codex CLI nao encontrado.' -ForegroundColor Yellow
  Write-Host 'Instale com: npm install -g @openai/codex' -ForegroundColor Yellow
  exit 1
}

Write-Host 'Abrindo o fluxo oficial de login do Codex CLI...' -ForegroundColor Green
& codex --login

Write-Host ''
Write-Host 'Observacao: este login autentica o Codex CLI para desenvolvimento.' -ForegroundColor Yellow
Write-Host 'Ele nao substitui NVIDIA_API_KEY usada pelo Spring AI/NVIDIA NIM.' -ForegroundColor Yellow
