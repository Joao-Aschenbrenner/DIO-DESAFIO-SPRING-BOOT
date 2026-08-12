# DIO Desafio Spring Boot + Spring AI

Projeto desenvolvido para o desafio final da trilha Spring Boot da DIO, evoluindo a proposta de uma **API inteligente de orçamento** que recebe comandos financeiros, integra IA com casos de uso reais e mantém separação arquitetural entre domínio, aplicação e infraestrutura.

## O que o projeto faz

A aplicação funciona como um assistente financeiro. Ela permite registrar e consultar despesas por REST e também disponibiliza endpoints de IA para interpretar comandos em linguagem natural e áudio.

Fluxo principal implementado:

```text
Áudio
  ↓
TranscriptionModel
  ↓
Texto transcrito
  ↓
ChatClient + Tool Calling
  ↓
CreateTransactionUseCase / ListTransactionsUseCase
  ↓
TransactionRepository
  ↓
H2 / JPA
  ↓
Resposta da IA
```

## Evoluções implementadas

Além da base conceitual apresentada no desafio, esta versão adiciona:

- domínio próprio com `Transaction`, `TransactionId` fortemente tipado e `Category`;
- separação DDD em `domain`, `application` e `infrastructure`;
- casos de uso individuais para criação e consulta de transações;
- persistência real via Spring Data JPA + H2;
- API REST tradicional para criar e consultar despesas;
- `ChatClient` configurado com tools reais da aplicação;
- Tool Calling para registrar e listar despesas;
- endpoint de comando textual com IA;
- endpoint de transcrição de áudio;
- endpoint integrado de comando por voz: transcreve o áudio e envia a intenção ao `ChatClient`;
- validações de entrada;
- teste unitário do caso de uso sem depender de API externa;
- GitHub Actions para executar os testes automaticamente.

## Arquitetura

```text
br.com.jaaschenbrenner.budgetai
├── domain
│   ├── Transaction.java
│   ├── TransactionId.java
│   ├── Category.java
│   └── TransactionRepository.java
├── application
│   ├── CreateTransactionInput.java
│   ├── CreateTransactionUseCase.java
│   ├── ListTransactionsUseCase.java
│   └── TransactionOutput.java
└── infrastructure
    ├── ai
    │   ├── AiConfiguration.java
    │   ├── AiController.java
    │   ├── FinanceTools.java
    │   ├── TranscriptionController.java
    │   └── VoiceCommandController.java
    ├── config
    │   └── UseCaseConfiguration.java
    ├── persistence
    │   ├── TransactionEntity.java
    │   ├── SpringDataTransactionRepository.java
    │   └── JpaTransactionRepositoryAdapter.java
    └── web
        └── TransactionController.java
```

A camada **Domain** contém o modelo e as regras fundamentais. A **Application** coordena os casos de uso. A **Infrastructure** contém REST, Spring AI, banco de dados e integrações externas.

## Tecnologias

- Java 21
- Spring Boot 4
- Spring AI 2
- Spring Web
- Spring Data JPA
- Bean Validation
- OpenAI via Spring AI
- ChatClient
- Tool Calling
- TranscriptionModel
- H2 Database
- Gradle
- JUnit 5
- GitHub Actions

## Como executar

### 1. Pré-requisitos

- JDK 21+
- Gradle instalado
- chave da OpenAI para os endpoints de IA

### 2. Configure a chave

No PowerShell:

```powershell
$env:OPENAI_API_KEY="sua-chave-aqui"
```

No Linux/macOS:

```bash
export OPENAI_API_KEY="sua-chave-aqui"
```

**Nunca coloque a chave real no repositório.**

### 3. Execute os testes

```bash
gradle test
```

O teste de domínio/aplicação não depende da OpenAI.

### 4. Inicie a aplicação

```bash
gradle bootRun
```

Por padrão:

```text
http://localhost:8080
```

## Como testar

### Criar uma despesa sem IA

```http
POST /api/transactions
Content-Type: application/json

{
  "description": "Almoço",
  "amountInCents": 4590,
  "category": "ALIMENTACAO"
}
```

### Listar despesas

```http
GET /api/transactions
```

Filtrando por categoria:

```http
GET /api/transactions?category=ALIMENTACAO
```

### Comando textual com IA

```http
POST /api/ai/command
Content-Type: application/json

{
  "text": "Registre 50 reais de combustível na categoria transporte"
}
```

O modelo pode decidir chamar a tool `registrarDespesa`, que executa um caso de uso real e grava a transação no banco.

Outro exemplo:

```json
{
  "text": "Liste minhas despesas de alimentação"
}
```

### Transcrever um áudio

Envie um arquivo multipart para:

```text
POST /api/ai/transcribe
campo: file
```

### Executar um comando financeiro por voz

Envie um arquivo multipart para:

```text
POST /api/ai/voice-command
campo: file
```

Esse endpoint executa:

```text
arquivo → transcrição → ChatClient → Tool Calling → caso de uso → banco → resposta
```

Exemplo de fala:

> "Gastei cinquenta reais no posto com combustível."

## Segurança da chave de IA

O projeto usa:

```properties
spring.ai.openai.api-key=${OPENAI_API_KEY:}
```

Assim a credencial fica fora do Git. O arquivo `.gitignore` também ignora `.env`.

## Testes e validação

O repositório possui um teste unitário para garantir que o caso de uso de criação:

- persista a transação por meio do contrato `TransactionRepository`;
- mantenha o valor internamente em centavos;
- converta corretamente o valor para a saída monetária;
- funcione sem Spring, banco de dados ou IA.

O GitHub Actions executa `gradle test` a cada push e pull request.

## Relação com o projeto da DIO

Este projeto foi criado para a entrega do desafio final de Spring AI da DIO e segue os conceitos apresentados na trilha:

- DDD;
- Clean Architecture / Use Cases;
- Spring Web;
- Spring Data;
- integração com serviços externos;
- Spring AI;
- `ChatClient`;
- Tool Calling;
- Speech-to-Text.

A implementação deste repositório foi evoluída como uma solução própria para demonstrar os conceitos, em vez de simplesmente reproduzir o projeto do expert.

Projeto de referência da trilha:

https://github.com/digitalinnovationone/dio-spring-boot-learning-track/tree/main/05-spring-ai

## O que ainda pode ser evoluído

- Text-to-Speech para fechar o ciclo áudio → áudio;
- relatórios por período;
- novas tools para totais e estatísticas;
- testes de integração reais do Spring AI condicionados à presença de `OPENAI_API_KEY`;
- Docker Compose com MySQL;
- autenticação e associação das transações a usuários;
- documentação OpenAPI/Swagger;
- auditoria dos comandos interpretados pela IA.

## O que aprendi

O principal aprendizado deste desafio é que integrar IA em uma aplicação real não significa colocar regras de negócio dentro do prompt. A IA interpreta a intenção e escolhe ferramentas, enquanto os casos de uso Java continuam responsáveis por executar as operações reais do sistema. Essa separação mantém o projeto testável, organizado e mais fácil de evoluir.
