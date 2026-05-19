version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: database-container
    environment:
      POSTGRES_DB: apiforge_db
      POSTGRES_USER: apiforge_user
      POSTGRES_PASSWORD: apiforge_password
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U apiforge_user -d apiforge_db"]
      interval: 5s
      timeout: 5s
      retries: 5
    networks:
      - apiforge-net

  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: application-container
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/apiforge_db
      - SPRING_DATASOURCE_USERNAME=apiforge_user
      - SPRING_DATASOURCE_PASSWORD=apiforge_password
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - apiforge-net

volumes:
  pgdata:

networks:
  apiforge-net:
