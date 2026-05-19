# Base Spring Properties
spring.application.name=generated-api

# Datasource Settings (with environmental fallbacks)
spring.datasource.url=${r"${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/apiforge_db}"}
spring.datasource.username=${r"${SPRING_DATASOURCE_USERNAME:apiforge_user}"}
spring.datasource.password=${r"${SPRING_DATASOURCE_PASSWORD:apiforge_password}"}
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA / Hibernate Configuration
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# Flyway Migration Configuration
spring.flyway.enabled=${r"${SPRING_FLYWAY_ENABLED:true}"}
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration

# Server Options
server.port=${r"${SERVER_PORT:8080}"}
