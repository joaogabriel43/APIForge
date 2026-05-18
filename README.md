# APIForge Server

APIForge is a robust, developer-first backend system designed to dynamically generate ready-to-run REST APIs from SQL schemas.

## 🚀 Overview
APIForge reads, parses, and translates complex SQL database schemas into modular, production-ready Spring Boot REST API codebases or serving endpoints.

## 🛠️ Stack and Technologies
- **Core Platform**: Java 21 & Spring Boot 3.2.11
- **SQL Parser**: JSQLParser
- **Database**: PostgreSQL (with Flyway Migrations & Testcontainers for testing)
- **Profiling**: P6Spy (SQL formatting & N+1 protection)
- **Testing**: JUnit 5, Mockito & jqwik (Property-based testing)

## 🏛️ Architecture
This project strictly follows **Clean Architecture** patterns, ensuring modularity, isolation of business logic, and high testability:
- **`domain`**: Pure Java business rules, entities, and port interfaces. Free from framework annotations.
- **`application`**: Application use cases orchestrating domains and ports.
- **`infrastructure`**: Concrete details (Spring configuration, JPA entity adapters, P6Spy and database drivers).
- **`presentation`**: Spring RestControllers, request/response DTOs, and global exception mappers.

## ⚡ Getting Started
1. Copy `.env.example` to `.env` and fill in your PostgreSQL credentials.
2. Compile the project:
   ```bash
   mvn clean compile
   ```
3. Run tests (including property-based tests):
   ```bash
   mvn clean test
   ```
