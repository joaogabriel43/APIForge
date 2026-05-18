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

---

## 🐛 Erros Conhecidos
*(Esta seção está vazia atualmente. Registre bugs críticos e resoluções conhecidas aqui conforme surgirem durante as Sprints).*
