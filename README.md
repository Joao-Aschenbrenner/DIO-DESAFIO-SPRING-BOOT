# DIO Desafio Spring Boot + Spring AI

Assistente financeiro desenvolvido para o desafio final da trilha Spring Boot + Spring AI da DIO.

## Fluxo principal

```text
Áudio
  ↓
Transcrição
  ↓
ChatClient + Tool Calling
  ↓
Use Cases Java
  ↓
Spring Data JPA / H2
  ↓
Resposta
```

A arquitetura mantém as camadas `domain`, `application` e `infrastructure`, deixando as regras de negócio fora do prompt e dos controllers.

## IA padrão: NVIDIA NIM

O ChatClient e o Tool Calling usam por padrão a API OpenAI-compatible do **NVIDIA NIM**.

Configuração padrão:

```text
Base URL: https://integrate.api.nvidia.com
Modelo:   z-ai/glm-5.2
Chave:    NVIDIA_API_KEY
```

O modelo pode ser trocado sem alterar o código:

```powershell
$env:NVIDIA_MODEL="nvidia/nemotron-3-super-120b-a12b"
```

Nenhuma chave é salva no repositório.

## Áudio

O LLM de chat é NVIDIA NIM. A transcrição de áudio está isolada e, nesta versão, continua usando o `TranscriptionModel` compatível com OpenAI:

```powershell
$env:OPENAI_API_KEY="sua-chave"
```

Sem `OPENAI_API_KEY`, o restante da aplicação continua disponível e os comandos de texto podem ser testados com NVIDIA NIM.

## Codex

A release inclui `codex-login.ps1` e `codex-login.cmd` para abrir o fluxo oficial de autenticação do Codex CLI.

O login do Codex é destinado ao uso do Codex como agente de desenvolvimento do projeto. Ele é separado da autenticação do Spring AI e não substitui `NVIDIA_API_KEY`.

## Release para Windows

A release `v0.2.0-nvidia` inclui:

- `budget-ai.jar`;
- JRE 21 embutido;
- `start-nvidia.cmd`;
- `start-nvidia.ps1`;
- `codex-login.cmd`;
- `codex-login.ps1`;
- painel web local em `http://localhost:8080`.

Para testar:

1. baixe e extraia o ZIP da release;
2. execute `start-nvidia.cmd`;
3. cole sua NVIDIA API Key quando solicitado;
4. o navegador abrirá `http://localhost:8080`;
5. teste comandos como `Registre uma despesa de 42 reais com mercado`.

## Endpoints

```text
GET  /api/system/ai-provider
POST /api/ai/command
POST /api/ai/transcribe
POST /api/ai/voice-command
GET  /api/transactions
POST /api/transactions
```

## Recursos implementados

- DDD: Domain, Application e Infrastructure;
- `Transaction`, `TransactionId` fortemente tipado e `Category`;
- Use Cases separados para criar e consultar transações;
- Spring Data JPA + H2 persistente;
- API REST;
- Spring AI `ChatClient`;
- NVIDIA NIM como provedor de chat;
- Tool Calling com funções reais da aplicação;
- transcrição de áudio;
- comando financeiro por voz;
- painel web simples para teste;
- validações;
- testes automatizados;
- GitHub Actions.

## Desenvolvimento local

Pré-requisitos: Java 21 e Gradle.

```powershell
$env:NVIDIA_API_KEY="sua-chave-nvidia"
gradle test
gradle bootRun
```

Para habilitar áudio:

```powershell
$env:OPENAI_API_KEY="sua-chave-openai"
```

## Projeto de referência da DIO

https://github.com/digitalinnovationone/dio-spring-boot-learning-track/tree/main/05-spring-ai

Esta implementação usa o projeto da trilha como referência de conceitos, mas foi estruturada e evoluída como uma solução própria para a entrega.

## Próximas evoluções

- Text-to-Speech para fechar o ciclo áudio → áudio;
- ASR NVIDIA NIM dedicado;
- relatórios por período;
- novas tools financeiras;
- Docker Compose + MySQL;
- autenticação de usuários;
- auditoria de comandos da IA.
