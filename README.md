<p align="center">
  <img src="src/main/resources/static/logo.svg" width="150" alt="Budget AI logo">
</p>

# Budget AI — Desafio DIO Spring Boot + Spring AI

Assistente financeiro desenvolvido como evolução do projeto final da trilha **Santander 2026 — Java Backend**, disponibilizada pela DIO.

**Curso / trilha:** https://web.dio.me/track/santander-2026-java-backend

A proposta é demonstrar, na prática, os conceitos apresentados no curso para integrar **Spring Boot**, **Spring AI**, modelos de IA, transcrição de áudio e **Tool Calling** sem colocar regra de negócio dentro do modelo: a IA entende a intenção e escolhe ferramentas; as operações reais continuam nos casos de uso Java e na persistência da aplicação.

## Links da entrega

- **Repositório:** https://github.com/Joao-Aschenbrenner/DIO-DESAFIO-SPRING-BOOT
- **Releases:** https://github.com/Joao-Aschenbrenner/DIO-DESAFIO-SPRING-BOOT/releases
- **Release Windows v0.3.5:** https://github.com/Joao-Aschenbrenner/DIO-DESAFIO-SPRING-BOOT/releases/tag/v0.3.5-windows
- **Instalador Windows x64:** https://github.com/Joao-Aschenbrenner/DIO-DESAFIO-SPRING-BOOT/releases/download/v0.3.5-windows/BudgetAI-Setup-v0.3.5-Windows-x64.exe
- **Trilha Santander 2026 — Java Backend:** https://web.dio.me/track/santander-2026-java-backend
- **Projeto de referência Spring AI da DIO:** https://github.com/digitalinnovationone/dio-spring-boot-learning-track/tree/main/05-spring-ai

## O que o projeto faz

O Budget AI permite registrar e consultar despesas por **voz**, **texto** ou por um endpoint REST tradicional.

```text
Áudio
  ↓
NVIDIA Nemotron Omni
  ↓ transcrição
Spring AI ChatClient
  ↓ intenção
Tool Calling
  ↓
Use Cases Java
  ↓
JPA / H2
  ↓
Resposta final
  ├─ texto
  ├─ MP3 via Spring AI TextToSpeechModel (quando configurado)
  └─ TTS local do navegador como fallback
```

## Como esta entrega cobre o desafio

| Requisito | Implementação |
|---|---|
| Spring Boot | API e aplicação local em Java 21 |
| Spring AI | `ChatClient` + Tool Calling |
| Áudio → texto | NVIDIA Nemotron Omni |
| Entender intenção | modelo NVIDIA através da compatibilidade OpenAI do Spring AI |
| Executar função real | `FinanceTools` chama os mesmos use cases da API REST |
| Criar/consultar transações | `CreateTransactionUseCase` e `ListTransactionsUseCase` |
| Persistência | Spring Data JPA + H2 local |
| Texto → voz | `TextToSpeechModel` opcional, retornando MP3 |
| Fallback de voz | Web Speech API local em pt-BR |
| Endpoints REST | transações, IA, transcrição e TTS |
| Testes | domínio, contexto, contratos de UI/Tool Calling e TTS |
| Documentação | este README + interface demonstrativa |

## Evoluções implementadas

Além do fluxo base do desafio, esta versão adiciona:

- interface web responsiva e explicativa;
- fluxo visual das cinco etapas do desafio;
- NVIDIA NIM como provedor principal de texto e áudio de entrada;
- uma única chave NVIDIA para Chat, Tool Calling e transcrição;
- TTS real e opcional através da interface `TextToSpeechModel` do Spring AI;
- fallback local de voz quando o TTS cloud não está configurado;
- tratamento centralizado de erros com `correlationId`;
- validações no REST **e também no domínio**, para que Tool Calling não contorne regras;
- categorias compartilhadas entre backend e interface;
- valores manuais digitados em reais e convertidos para centavos;
- servidor preso a `127.0.0.1`;
- console H2 desligado por padrão;
- launcher Windows com Java 21 embutido;
- instalador/atalho com ícone próprio;
- logs de diagnóstico sem gravar as chaves de API.

## Interface

A interface foi organizada para demonstrar o projeto para alguém que não conhece o código:

1. **Fluxo por voz** é a ação principal.
2. A tela mostra as etapas áudio → transcrição → Spring AI → Tool Calling → resposta.
3. O retorno exibe texto legível, não JSON cru.
4. As transações aparecem em uma lista com valor em reais, categoria e data.
5. O cadastro manual fica como alternativa para testar o endpoint REST sem IA.
6. Informações de provedor/modelo ficam em **Detalhes técnicos**.
7. O layout possui regras de reflow para telas estreitas e não depende de uma largura mínima de card.

## Arquitetura

```text
src/main/java/br/com/jaaschenbrenner/budgetai
├── domain
│   ├── Transaction
│   ├── TransactionId
│   ├── Category
│   └── TransactionRepository
├── application
│   ├── CreateTransactionUseCase
│   ├── ListTransactionsUseCase
│   └── TransactionOutput
└── infrastructure
    ├── ai
    │   ├── AiConfiguration
    │   ├── AiController
    │   ├── FinanceTools
    │   ├── NvidiaOmniAudioClient
    │   ├── SpeechController
    │   ├── TranscriptionController
    │   └── VoiceCommandController
    ├── persistence
    ├── config
    └── web
```

A camada de domínio não depende de Spring AI. A IA entra como adaptador em `infrastructure/ai` e chama casos de uso da camada `application`.

## Tool Calling

As tools expostas ao modelo são operações reais:

- `registrarDespesa(...)`
- `listarDespesas(...)`

A categoria da consulta é opcional para que a IA consiga listar **todas** as despesas sem inventar um filtro. As descrições das tools também informam explicitamente as categorias válidas e o formato do valor em centavos.

Categorias do domínio:

```text
ALIMENTACAO
TRANSPORTE
SAUDE
LAZER
MORADIA
EDUCACAO
OUTROS
```

## IA e transcrição com NVIDIA

Configuração principal:

```text
Provider: NVIDIA NIM
Base URL: https://integrate.api.nvidia.com
Modelo:   nvidia/nemotron-3-nano-omni-30b-a3b-reasoning
Chave:    NVIDIA_API_KEY
```

Arquivos WAV e MP3 são aceitos, com limite de 50 MB.

Áudios pequenos são enviados inline. Para arquivos maiores, o cliente cria um asset temporário NVIDIA, envia o arquivo por HTTPS, usa a referência no modelo e tenta remover o asset ao terminar.

## Text-to-Speech

A saída de voz possui dois níveis.

### 1. Spring AI `TextToSpeechModel` — opcional

Se uma chave OpenAI for informada no campo opcional do launcher:

```text
OPENAI_TTS_API_KEY=...
BUDGETAI_TTS_PROVIDER=openai
```

o Spring AI habilita um `TextToSpeechModel`. O endpoint:

```text
POST /api/ai/speech
Content-Type: application/json
Accept: audio/mpeg
```

gera um MP3 a partir da resposta textual.

### 2. Fallback local

Se nenhuma chave de TTS for configurada, o projeto continua totalmente funcional. A interface usa a Web Speech API disponível no navegador e mantém a resposta em texto caso síntese de voz não esteja disponível.

A chave NVIDIA continua sendo a única credencial **obrigatória**.

## Tratamento de erros

A API usa `@RestControllerAdvice` e retorna uma estrutura consistente:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Revise os campos informados.",
  "path": "/api/transactions",
  "correlationId": "uuid",
  "details": [
    "description: não deve estar em branco"
  ]
}
```

Há tratamento específico para validação de body, JSON/categoria inválidos, parâmetros inválidos, arquivo obrigatório ausente, arquivo acima de 50 MB, formato HTTP não suportado, endpoint/método inválido, erro de persistência, erro do provedor NVIDIA, erro do TTS, TTS não configurado, I/O e erro inesperado.

Stack traces ficam somente no log. A interface mostra primeiro uma mensagem humana e deixa código/correlação dentro de **detalhes técnicos**.

## Robustez e segurança local

- `server.address=127.0.0.1`: a API não fica exposta para a LAN.
- H2 Console desligado por padrão.
- As chaves do launcher não são gravadas em arquivo.
- O upload temporário de áudio exige URL HTTPS e rejeita destinos locais.
- O domínio valida descrição, categoria e valor mesmo quando a operação vem de Tool Calling.
- O launcher detecta porta ocupada, banco em uso e falhas de inicialização e aponta o arquivo de log.
- O TTS opcional não pode impedir o boot quando não está configurado.

## Endpoints

```text
GET  /api/system/ai-provider

POST /api/ai/command
POST /api/ai/transcribe
POST /api/ai/voice-command

GET  /api/ai/speech/status
POST /api/ai/speech

GET  /api/transactions
GET  /api/transactions/categories
POST /api/transactions
```

## Como executar em desenvolvimento

Pré-requisitos:

- Java 21
- Gradle
- NVIDIA NIM API Key

PowerShell:

```powershell
$env:NVIDIA_API_KEY="sua-chave-nvidia"
gradle clean test
gradle bootRun
```

Acesse:

```text
http://127.0.0.1:8080
```

### TTS Spring AI opcional

```powershell
$env:OPENAI_TTS_API_KEY="sua-chave-openai"
$env:BUDGETAI_TTS_PROVIDER="openai"
gradle bootRun
```

Sem essas duas variáveis, a aplicação inicia normalmente e usa TTS local no navegador.

## Como testar o fluxo principal

### Voz

1. Abra **Experimente o fluxo completo por voz**.
2. Selecione um WAV ou MP3.
3. Use uma frase como: `Gastei 42 reais no mercado`.
4. Clique em **Processar comando por voz**.
5. Confira a transcrição.
6. Confira a resposta da IA.
7. Veja a transação aparecer em **Transações salvas**.
8. Use **Ouvir resposta**.

### Texto

```text
Registre uma despesa de 25 reais com Uber.
Liste todas as minhas despesas.
Quais são meus gastos com alimentação?
```

### REST manual

Abra **Adicionar uma transação manualmente**, informe um valor como `12,50`, categoria e descrição. Esse fluxo testa o endpoint REST sem passar pela IA.

## Testes automatizados

Execute:

```bash
gradle clean test
```

A suíte cobre, entre outros pontos, criação de transação, invariantes do domínio, inicialização do ApplicationContext, categorias da interface alinhadas ao enum Java, parâmetro opcional da tool de listagem, TTS Spring AI + fallback local, comportamento do `SpeechController`, contrato de responsividade/explicação da UI e configurações locais de segurança.

O workflow Windows também executa testes antes de montar o instalador.

## Executável Windows

O instalador é produzido com `jpackage` e inclui Java 21 embutido, launcher gráfico, ícone próprio Budget AI, backend Spring Boot, criação de atalho/menu e banco/logs em `%LOCALAPPDATA%\BudgetAI`.

O launcher solicita:

- **NVIDIA NIM API Key** — obrigatória;
- **OpenAI API Key para TTS** — opcional.

### Release atual

**Budget AI v0.3.5 — Delivery Candidate**

- Release: https://github.com/Joao-Aschenbrenner/DIO-DESAFIO-SPRING-BOOT/releases/tag/v0.3.5-windows
- Instalador: https://github.com/Joao-Aschenbrenner/DIO-DESAFIO-SPRING-BOOT/releases/download/v0.3.5-windows/BudgetAI-Setup-v0.3.5-Windows-x64.exe
- Todas as releases: https://github.com/Joao-Aschenbrenner/DIO-DESAFIO-SPRING-BOOT/releases

## O que aprendi / decisões do projeto

Os principais aprendizados desta evolução foram:

- aplicar IA de forma integrada a uma aplicação Java real, seguindo os conceitos apresentados na trilha Santander 2026 — Java Backend;
- a IA deve decidir **qual operação** executar, mas a regra real pertence à aplicação;
- Tool Calling precisa de schemas e parâmetros bem descritos para reduzir chamadas erradas;
- validação só no controller não é suficiente, porque uma tool pode chamar o caso de uso diretamente;
- integração de IA precisa de tratamento de indisponibilidade, timeout e respostas vazias;
- uma interface de demonstração deve explicar o fluxo, não apenas imprimir JSON;
- recursos opcionais, como TTS cloud, não devem derrubar a aplicação quando não estão configurados.

## Curso e projeto de referência da DIO

Este projeto foi desenvolvido para a trilha:

**Santander 2026 — Java Backend**  
https://web.dio.me/track/santander-2026-java-backend

Os conceitos de Spring AI aplicados nesta entrega seguem os ensinamentos e a base disponibilizada no curso, especialmente o projeto final de Spring AI:

https://github.com/digitalinnovationone/dio-spring-boot-learning-track/tree/main/05-spring-ai

Esta implementação é uma evolução própria para fins de estudo e entrega do desafio, incluindo interface responsiva, NVIDIA NIM, tratamento de erros, testes, TTS e instalador Windows.
