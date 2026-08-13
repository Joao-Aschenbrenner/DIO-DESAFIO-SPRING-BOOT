# DIO Desafio Spring Boot + Spring AI

Assistente financeiro desenvolvido para o desafio final da trilha Spring Boot + Spring AI da DIO.

## Fluxo principal

```text
Texto ───────────────┐
                    ↓
Áudio → NVIDIA Omni → ChatClient + Tool Calling → Use Cases Java → JPA/H2 → resposta
```

A arquitetura mantém as camadas `domain`, `application` e `infrastructure`. A IA interpreta a intenção e escolhe ferramentas, enquanto as regras e operações reais continuam nos casos de uso Java.

## Uma única API NVIDIA

A versão atual usa a mesma credencial, o mesmo modelo e o mesmo endpoint NVIDIA para chat, Tool Calling e entrada de áudio:

```text
Provider: NVIDIA NIM
Base URL: https://integrate.api.nvidia.com
Endpoint: /v1/chat/completions
Modelo:   nvidia/nemotron-3-nano-omni-30b-a3b-reasoning
Chave:    NVIDIA_API_KEY
```

O fluxo de áudio não precisa mais de `OPENAI_API_KEY`. Arquivos WAV e MP3 são enviados ao Nemotron Omni. Áudios pequenos podem ir inline; arquivos maiores usam temporariamente o NVIDIA Cloud Functions Asset API e o asset é removido após o processamento.

> Observação de compatibilidade: o model card oficial da NVIDIA informa suporte de idioma **English only**. O suporte prático a fala em português deve ser validado durante os testes da aplicação.

## Executável Windows

A release Windows contém um instalador `.exe` com Java 21 embutido. Depois da instalação, abra **BudgetAI** pelo menu/atalho do Windows.

O launcher:

- solicita somente a `NVIDIA_API_KEY`;
- usa o modelo NVIDIA Omni fixado e compatível com áudio + texto + tools;
- inicia o backend Spring Boot automaticamente;
- abre o painel local em `http://localhost:8080`;
- mantém a chave somente na memória do processo;
- possui botão para abrir o Codex.

## Codex

O botão **Login Codex** não depende mais de npm. Se o Codex CLI não estiver instalado, o launcher executa o instalador oficial para Windows da OpenAI e depois abre o Codex para que o usuário escolha **Sign in with ChatGPT**.

O login do Codex é separado da `NVIDIA_API_KEY`: Codex é uma ferramenta de desenvolvimento, enquanto a aplicação Spring AI usa NVIDIA NIM em runtime.

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

- Java 21 + Spring Boot 4;
- Spring AI `ChatClient`;
- DDD: Domain, Application e Infrastructure;
- `Transaction`, `TransactionId` e `Category`;
- Use Cases separados;
- Spring Data JPA + H2 persistente;
- REST API;
- NVIDIA NIM;
- Nemotron Omni para texto e áudio;
- Tool Calling com funções reais da aplicação;
- transcrição de WAV/MP3;
- comando financeiro por voz;
- painel web local;
- validações;
- testes automatizados;
- GitHub Actions;
- instalador Windows com runtime Java embutido.

## Desenvolvimento local

Pré-requisitos: Java 21 e Gradle.

```powershell
$env:NVIDIA_API_KEY="sua-chave-nvidia"
gradle test
gradle bootRun
```

Depois acesse:

```text
http://localhost:8080
```

Exemplo de comando:

```text
Registre uma despesa de 42 reais com mercado.
```

Para áudio, envie um `.wav` ou `.mp3` pelo painel ou pelo endpoint `POST /api/ai/voice-command`.

## Projeto de referência da DIO

https://github.com/digitalinnovationone/dio-spring-boot-learning-track/tree/main/05-spring-ai

Esta implementação usa o projeto da trilha como referência de conceitos, mas foi estruturada e evoluída como uma solução própria para a entrega.

## Próximas evoluções

- Text-to-Speech para fechar o ciclo áudio → áudio;
- testar e documentar a qualidade de reconhecimento em português;
- relatórios financeiros por período;
- novas tools;
- Docker Compose + MySQL;
- autenticação e auditoria.
