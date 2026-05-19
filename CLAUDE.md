# CLAUDE.md - APIForge Memória Persistente

Este documento serve como memória persistente e guia definitivo de desenvolvimento para o projeto **APIForge**. Ele estabelece as regras arquiteturais, decisões técnicas (ADRs), padrões de projeto e definições da stack.

---

## 🚀 Visão Geral do Projeto
O **APIForge** é um gerador inteligente de APIs REST a partir de schemas SQL. O sistema lê, analisa e interpreta definições de tabelas SQL (DDLs) e gera automaticamente estruturas de APIs completas, modulares, padronizadas e prontas para execução sob o framework Spring Boot.

---

## 🏛️ Estrutura de Diretórios (Clean Architecture)

A estrutura de pacotes principal reside em `src/main/java/com/apiforge/` e reflete estritamente as camadas da **Clean Architecture**:

```
com.apiforge/
│
├── ApiForgeApplication.java    # Classe de bootstrapping do Spring Boot (Root)
│
├── domain/                     # Camada de Domínio (Pureza de Negócios)
│   ├── model/                  # Entidades de negócio e Objetos de Valor
│   ├── exception/              # Exceções de regras de negócio
│   └── repository/             # Interfaces (Ports) de persistência e serviços externos
│
├── application/                # Camada de Aplicação (Casos de Uso)
│   ├── usecase/                # Implementação dos fluxos de casos de uso
│   ├── dto/                    # Modelos de transferência interna (Commands/Queries)
│   └── exception/              # Exceções específicas da aplicação
│
├── infrastructure/             # Camada de Infraestrutura (Detalhes Técnicos)
│   ├── config/                 # Beans de configuração (Spring, P6Spy, etc.)
│   ├── db/                     # Entidades JPA, Repositórios Spring Data e Flyway Migrations
│   └── adapter/                # Implementações concretas de serviços externos / persistência
│
└── presentation/               # Camada de Apresentação (Interface Externa)
    ├── controller/             # Controllers REST do Spring MVC exposing endpoints
    ├── dto/                    # DTOs de Request e Response da API
    ├── mapper/                 # Conversores entre DTOs de Apresentação e DTOs de Casos de Uso
    └── exception/              # Handler global de exceções e mapeador de HTTP Status
```

---

## 📋 Padrões do Projeto

1. **Clean Architecture Estrita**:
   * O **Domínio** é o centro e não tem conhecimento de frameworks, bancos de dados ou qualquer detalhe técnico externo (sem anotações Spring ou JPA).
   * A **Aplicação** gerencia os casos de uso e depende puramente do domínio e de interfaces (Ports).
   * A **Infraestrutura** implementa os adaptadores concretos e lida com frameworks diretamente.
   * A **Apresentação** expõe endpoints REST e converte os payloads para chamadas de aplicação.
2. **TDD (Test-Driven Development)**:
   * Escreva os testes unitários e de propriedades antes ou em paralelo às regras de negócio.
   * Utilize `jqwik` para descobrir falhas de borda por meio de testes baseados em propriedades.
3. **YAGNI (You Aren't Gonna Need It)**:
   * Evite implementar código antecipadamente sem um caso de uso claro associado. Sem "futurismo" de código.
4. **Tratamento de Exceções**:
   * Exceções do domínio ou aplicação não podem vazar diretamente HTTP Status para fora. O `presentation` captura as exceções e mapeia em DTOs estruturados padrão RFC 7807 (Problem Details).

---

## 📦 Dependências Críticas

| Dependência | Versão | Justificativa |
| :--- | :--- | :--- |
| **Java** | `21` | Versão LTS moderna com suporte a Pattern Matching, Records estáveis e melhorias de performance. |
| **Spring Boot** | `3.2.11` | Framework corporativo robusto para bootstrap, injeção de dependência e suporte REST. |
| **JSQLParser** | `4.8` | Biblioteca de parsing de SQL que lê e interpreta DDLs de forma precisa para a extração do schema. |
| **P6Spy Starter** | `1.9.1` | Interceptador JDBC para profiling de SQL queries em tempo de execução, vital para detectar problemas de performance como N+1. |
| **jqwik** | `1.8.5` | Framework de Property-based Testing integrado ao JUnit 5 para validações matemáticas e de integridade dos DDLs analisados. |
| **Flyway** | `3.2` parent | Ferramenta de versionamento e migração de banco de dados PostgreSQL estruturado e resiliente. |
| **FreeMarker** | `2.3` parent starter | Motor de templates para renderização desacoplada do código-fonte das APIs geradas. |

---

## 📄 ADRs (Architecture Decision Records)

### ADR 001: Escolha da Stack e Linguagem (Java 21 + Spring Boot 3.2)
* **Status**: Aprovado
* **Contexto**: O gerador de APIs requer uma linguagem robusta com tipagem estática forte para validação de metadados de schemas e frameworks consolidados para criar soluções enterprise estáveis.
* **Decisão**: Usar Java 21 LTS e Spring Boot 3.2.11.
* **Consequências**: Facilidade de manutenibilidade, uso de Records nativos para DTOs e Casos de Uso, Virtual Threads disponíveis se necessário no futuro e compatibilidade total com ecossistema Spring.

### ADR 002: Adoção de Arquitetura Limpa (Clean Architecture) Estrita
* **Status**: Aprovado
* **Contexto**: Como a essência do projeto envolve interpretar regras SQL e gerar novos códigos de API, precisamos manter o mecanismo gerador de regras totalmente desacoplado da tecnologia de entrega (HTTP, Banco de Dados Relacional local, etc.).
* **Decisão**: Dividir o sistema em quatro camadas estritas (`domain`, `application`, `infrastructure`, `presentation`).
* **Consequências**: Testabilidade isolada sem mocks complexos de Spring Boot, flexibilidade para expor o motor por CLI, GraphQL ou gRPC no futuro sem mudar as regras centrais.

### ADR 003: Uso de P6Spy para Prevenção de Query N+1
* **Status**: Aprovado
* **Contexto**: A geração de APIs dinâmicas com ORM (JPA/Hibernate) tende a produzir buscas SQL ineficientes ou múltiplos selects aninhados se os relacionamentos não forem bem mapeados.
* **Decisão**: Utilizar `p6spy-spring-boot-starter` no escopo de desenvolvimento local para imprimir SQLs estruturadas e monitorar queries redundantes.
* **Consequências**: Rápida identificação e correção de N+1 em tempo de desenvolvimento.

### ADR 004: jqwik para Validação de Parser SQL
* **Status**: Aprovado
* **Contexto**: O interpretador de SQL de entrada lidará com variações infinitas de queries e layouts SQL. Testes unitários comuns podem deixar escapar casos extremos de sintaxe SQL.
* **Decisão**: Adicionar `jqwik` para realizar testes baseados em propriedades gerando strings SQL arbitrárias para testar a robustez do parsing.
* **Consequências**: Maior resiliência contra exceções e quebras de interpretador em produção.

### ADR 005: Isolamento Estrito do Motor de Templates (FreeMarker)
* **Status**: Aprovado
* **Contexto**: A geração de código-fonte de múltiplos arquivos precisa ser limpa, flexível e totalmente desacoplada de concatenações manuais de strings ou regras Java acopladas.
* **Decisão**: Utilizar exclusivamente templates FreeMarker (`.ftl`) físicos no diretório `src/main/resources/templates/` para definir a estrutura sintática, injetando metadados por modelos de dados contextuais simples.
* **Consequências**: Código gerador limpo, modular, extensível e 100% livre de concatenações manuais de strings.

### ADR 006: Validação de Código Gerado com Stubs Dinâmicos e JavaCompiler
* **Status**: Aprovado
* **Contexto**: Garantir a correção absoluta das APIs geradas exige compilação programática estrita em tempo de execução. Contudo, dependências como Lombok, MapStruct e Jakarta Validation não devem constar no escopo compile do gerador.
* **Decisão**: Utilizar a Java Compiler API (`javax.tools.JavaCompiler`) rodando em diretórios temporários (`@TempDir`). Programar stubs leves para as anotações do Lombok, MapStruct e Jakarta no ambiente de testes, injetando construtores e accessores dinamicamente por preprocessamento AST para viabilizar compilação nativa pura sem vazamento de dependências.
* **Consequências**: Garantia absoluta de código gerado livre de erros de sintaxe ou imports inválidos com zero poluição nas dependências de produção do gerador.

### ADR 007: Pluralizador e Conversor de Nomenclatura Embutido (YAGNI)
* **Status**: Aprovado
* **Contexto**: A geração de rotas REST kebab-case plurais e nomes de classes/relacionamentos singulares PascalCase exige conversões de strings robustas sem introduzir dependências de NLP pesadas e desnecessárias.
* **Decisão**: Implementar um utilitário nativo puro (`NamingConventionService`) baseado em regras heurísticas de sufixo e tabelas de mapeamento leves.
* **Consequências**: Conversão rápida, previsível, testável e sem dependências externas adicionais.

### ADR 008: Empacotamento ZIP em Memória Independente de Plataforma
* **Status**: Aprovado
* **Contexto**: O empacotamento do projeto padrão Spring/Maven gerado para download exige portabilidade entre sistemas operacionais (Windows/Linux) e conformidade YAGNI.
* **Decisão**: Criar o `ZipGeneratorService` na infraestrutura utilizando a API nativa `java.util.zip.ZipOutputStream`, forçando normalização absoluta de barras (`/`) e stripping de caminhos iniciais.
* **Consequências**: Compactação rápida em memória livre de falhas de corrupção ou caminhos de SO incorretos, totalmente preparada para futuras exposições REST ou CLI.

### ADR 009: Adoção de SseEmitter em Thread-Pool Separada para Previews Real-time
* **Status**: Aprovado
* **Contexto**: O preview de código gerado em tempo real melhora drasticamente a experiência do usuário. Contudo, renderizar dezenas de arquivos de template sequencialmente com atrasos artificiais na thread HTTP do Tomcat causaria esgotamento rápido da thread pool do servlet.
* **Decisão**: Utilizar `SseEmitter` e desacoplar o processamento da thread original do Tomcat despachando a tarefa assíncrona por meio de `CompletableFuture.runAsync`.
* **Consequências**: Liberação imediata da thread servlet original para atender outras requisições HTTP, mantendo o servidor escalável sob alto volume de acessos paralelos e garantindo estabilidade no streaming de eventos em tempo real.

### ADR 010: Fire-and-Forget Seguro com @Async para Auditoria de Geração
* **Status**: Aprovado
* **Contexto**: A gravação de logs de auditoria no PostgreSQL é obrigatória a cada geração concluída. Contudo, oscilações ou falhas de persistência no banco de dados (banco offline, timeouts) jamais podem inviabilizar o download do arquivo ZIP de código gerado pelo cliente final.
* **Decisão**: Habilitar execução assíncrona no contexto com `@EnableAsync` e anotar o método de gravação de auditoria com `@Async`, isolando totalmente sua execução em um bloco `try-catch` robusto.
* **Consequências**: Persistência de logs totalmente transparente ao fluxo síncrono de geração de código (fire-and-forget), assegurando o download imediato dos arquivos ZIP mesmo diante de oscilações ou quedas totais da camada de persistência.

### ADR 011: Centralização e Padronização de Exceções com RFC 7807 (Problem Details)
* **Status**: Aprovado
* **Contexto**: Expor stack traces complexos ou mensagens técnicas brutas em requisições de erro (como falhas de parsing ou validações de payloads) é um risco grave de segurança (leaking de infraestrutura) e degrada a integração com o frontend.
* **Decisão**: Utilizar `@RestControllerAdvice` criando o `GlobalExceptionHandler` interceptando todas as falhas de API (DTOs, parâmetros GET, parsing SQL e Runtime fallbacks) e serializando as respostas estritamente sob o padrão internacional RFC 7807 (Problem Details).
* **Consequências**: Respostas de erro consistentes, fáceis de interpretar no frontend, totalmente sanitizadas contra vazamentos de logs de compiladores ou credenciais, com controle absoluto de HTTP Status por tipagem de erro.

---

## 🐛 Erros Conhecidos
*(Esta seção está vazia atualmente. Registre bugs críticos e resoluções conhecidas aqui conforme surgirem durante as Sprints).*
