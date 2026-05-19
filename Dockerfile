# Stage 1: Build the Maven application
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Download dependencies first to utilize Docker layer cache
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy sources and package compiled jar
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the runner image
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Install curl for reliable health checks in alpine
RUN apk add --no-cache curl

# Security: Create non-root group and user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy compiled jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Enforce non-root ownership on executable context
RUN chown -R spring:spring /app
USER spring:spring

EXPOSE 8080

# Spring Actuator Health Check
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
