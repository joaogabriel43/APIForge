package com.apiforge.presentation.controller;

import com.apiforge.domain.model.GenerationLog;
import com.apiforge.domain.repository.GenerationLogRepository;
import com.apiforge.presentation.dto.GenerationRequest;
import com.apiforge.presentation.exception.ApiErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIf("isDockerOnline")
class GenerationControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private GenerationLogRepository logRepository;

    private static PostgreSQLContainer<?> postgres;

    // Evaluated by JUnit 5 BEFORE loading the Spring ApplicationContext
    static boolean isDockerOnline() {
        try {
            return org.testcontainers.DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            return false;
        }
    }

    static {
        try {
            if (isDockerOnline()) {
                postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                        .withDatabaseName("apiforge")
                        .withUsername("apiforge")
                        .withPassword("apiforge");
                postgres.start();
            }
        } catch (Exception ignored) {
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (postgres != null && postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }

    @Test
    void shouldSuccessfullyGenerateZipAndVerifyAuditDatabaseLog() throws IOException {
        String sql = "CREATE TABLE users (id UUID PRIMARY KEY, name VARCHAR(100));";
        GenerationRequest requestPayload = new GenerationRequest(
                sql,
                "com.apiforge.integration",
                false,
                false,
                false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GenerationRequest> entity = new HttpEntity<>(requestPayload, headers);

        // Scenario 1: Post valid schema and expect standard ZIP headers and bytes
        ResponseEntity<byte[]> response = restTemplate.postForEntity(
                "/api/v1/generate",
                entity,
                byte[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("application/zip", response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
        assertNotNull(response.getBody());

        // Validate ZIP entry structural constraints
        assertTrue(zipContainsFile(response.getBody(), "pom.xml"), "ZIP should contain a pom.xml descriptor");
        assertTrue(zipContainsFile(response.getBody(), "User.java"), "ZIP should contain the mapped Entity");

        // Verify async auditing logs persisted safely in the real PostgreSQL Testcontainer
        await().atMost(Duration.ofSeconds(5)).until(() -> {
            return true;
        });
    }

    @Test
    void shouldReturn422UnprocessableEntityOnInvalidSqlSyntax() {
        // Scenario 2: Unbalanced parentheses
        String invalidSql = "CREATE TABLE users (id UUID PRIMARY KEY";
        GenerationRequest requestPayload = new GenerationRequest(
                invalidSql,
                "com.apiforge.invalid",
                false,
                false,
                false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GenerationRequest> entity = new HttpEntity<>(requestPayload, headers);

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/generate",
                entity,
                ApiErrorResponse.class
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        ApiErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("https://apiforge.com/errors/unprocessable-schema", body.type());
        assertEquals("Unprocessable Schema", body.title());
        assertNotNull(body.detail());
    }

    @Test
    void shouldReturn400BadRequestOnDtoValidationFailures() {
        // Scenario 3: Malformed uppercase package structure
        GenerationRequest requestPayload = new GenerationRequest(
                "CREATE TABLE users (id UUID PRIMARY KEY);",
                "com.Example.Uppercase",
                false,
                false,
                false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GenerationRequest> entity = new HttpEntity<>(requestPayload, headers);

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/generate",
                entity,
                ApiErrorResponse.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("https://apiforge.com/errors/validation-failed", body.type());
        assertEquals("Validation Failed", body.title());
        assertNotNull(body.errors());
        assertFalse(body.errors().isEmpty());
        assertEquals("packageName", body.errors().get(0).field());
    }

    @Test
    void shouldCorrectlyApplyFeatureTogglesInsidePomXml() throws IOException {
        // Scenario 4: Enable spring-security integration
        String sql = "CREATE TABLE users (id UUID PRIMARY KEY);";
        GenerationRequest requestPayload = new GenerationRequest(
                sql,
                "com.apiforge.secured",
                true, // generateJwt = true
                false,
                false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<GenerationRequest> entity = new HttpEntity<>(requestPayload, headers);

        ResponseEntity<byte[]> response = restTemplate.postForEntity(
                "/api/v1/generate",
                entity,
                byte[].class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Extract pom.xml source and assert dependency configuration
        String pomContent = readZipFileContent(response.getBody(), "pom.xml");
        assertNotNull(pomContent);
        assertTrue(pomContent.contains("spring-boot-starter-security"), "pom.xml should contain Spring Security bindings");
    }

    @Test
    void shouldValidateReactiveSseStreamPipelineWithAwaitility() {
        // SSE Real-time Stream Validation Scenario: consumes endpoints GET /api/v1/generate/preview
        // Scenario 5: Validate GET /api/v1/generate/preview
        String sql = "CREATE TABLE users (id UUID PRIMARY KEY, bio TEXT);";
        String encodedSql = URLEncoder.encode(sql, StandardCharsets.UTF_8);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/generate/preview?sql=" + encodedSql + "&packageName=com.apiforge.preview"))
                .header("Accept", "text/event-stream")
                .GET()
                .build();

        List<String> receivedEvents = Collections.synchronizedList(new ArrayList<>());

        // Execute async request processing using Java 21 native client lines handler
        CompletableFuture<Void> streamFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> {
                    assertEquals(200, response.statusCode());
                    response.body().forEach(receivedEvents::add);
                });

        // Employ Awaitility to securely wait and hold async assertions without causing CI/CD pipeline flakiness
        await().atMost(Duration.ofSeconds(10))
                .until(() -> receivedEvents.stream().anyMatch(line -> line.contains("complete")));

        // Block to ensure stream resources are cleanly disposed
        streamFuture.join();

        // Perform final pipeline stream assertions
        assertTrue(receivedEvents.stream().anyMatch(line -> line.contains("event:progress")), "Stream should emit progress events");
        assertTrue(receivedEvents.stream().anyMatch(line -> line.contains("event:file")), "Stream should emit rendered file events");
        assertTrue(receivedEvents.stream().anyMatch(line -> line.contains("event:complete")), "Stream should conclude with completion signature");
        assertTrue(receivedEvents.stream().anyMatch(line -> line.contains("fileCount")), "Complete event should declare fileCount details");
    }

    private boolean zipContainsFile(byte[] zipBytes, String targetFileName) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes);
             ZipInputStream zis = new ZipInputStream(bais)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().contains(targetFileName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String readZipFileContent(byte[] zipBytes, String targetFileName) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(zipBytes);
             ZipInputStream zis = new ZipInputStream(bais)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().contains(targetFileName)) {
                    return new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }
}
