package com.apiforge.infrastructure.adapter;

import com.apiforge.domain.model.ParsedSchema;
import com.apiforge.domain.model.RelationshipSuggestion;
import com.apiforge.domain.repository.LlmGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link OpenAiLlmGateway}.
 * Employs WireMock to intercept external OpenAI chat completions traffic,
 * and boots standard Spring Boot AOP contexts to verify Resilience4j aspects.
 */
@SpringBootTest
@EnabledIf("isDockerOnline")
class OpenAiLlmGatewayIT {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

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

    @Autowired
    private LlmGateway gateway;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Dynamically override the base API URL to point to the randomized local WireMock server
        registry.add("openai.api.url", wireMock::baseUrl);
        registry.add("openai.api.key", () -> "mock-openai-key");

        // Map PostgreSQL container credentials dynamically when active
        if (postgres != null && postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }

    @BeforeEach
    void resetResilienceStates() {
        // Reset the Circuit Breaker instance to a clean CLOSED state before each scenario execution
        circuitBreakerRegistry.circuitBreaker("openai").reset();
    }

    @Test
    void shouldSuccessfullyMapOpenAiResponsesToDomainEntities() throws Exception {
        // 1. Mock relationship suggestions response (array wrapped inside suggestions key)
        String relResponse = "{\n" +
                "  \"choices\": [\n" +
                "    {\n" +
                "      \"message\": {\n" +
                "        \"content\": \"{\\\"suggestions\\\": [{\\\"fromTable\\\": \\\"orders\\\", \\\"fromColumn\\\": \\\"user_id\\\", \\\"toTable\\\": \\\"users\\\", \\\"toColumn\\\": \\\"id\\\", \\\"type\\\": \\\"ManyToOne\\\"}]}\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson(relResponse)));

        ParsedSchema schema = new ParsedSchema(List.of(), List.of());
        List<RelationshipSuggestion> suggestions = gateway.inferRelationships(schema);

        assertNotNull(suggestions);
        assertEquals(1, suggestions.size());
        assertEquals("orders", suggestions.get(0).fromTable());
        assertEquals("user_id", suggestions.get(0).fromColumn());
        assertEquals("users", suggestions.get(0).toTable());
        assertEquals("id", suggestions.get(0).toColumn());
        assertEquals("ManyToOne", suggestions.get(0).type());

        // 2. Mock domain naming convention response
        String namingResponse = "{\n" +
                "  \"choices\": [\n" +
                "    {\n" +
                "      \"message\": {\n" +
                "        \"content\": \"{\\\"usr_id\\\": \\\"userId\\\", \\\"ord_dt\\\": \\\"orderDate\\\"}\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson(namingResponse)));

        Map<String, String> namingSuggestions = gateway.suggestDomainNames(List.of("usr_id", "ord_dt"));
        assertNotNull(namingSuggestions);
        assertEquals("userId", namingSuggestions.get("usr_id"));
        assertEquals("orderDate", namingSuggestions.get("ord_dt"));

        // 3. Mock generated Javadocs response
        String javadocResponse = "{\n" +
                "  \"choices\": [\n" +
                "    {\n" +
                "      \"message\": {\n" +
                "        \"content\": \"{\\\"User\\\": \\\"/**\\\\n * User entity domain definitions\\\\n */\\\"}\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(okJson(javadocResponse)));

        Map<String, String> javadocs = gateway.generateJavadoc(List.of("User"));
        assertNotNull(javadocs);
        assertTrue(javadocs.get("User").contains("User entity domain definitions"));
    }

    // Scenario 2: Validate HTTP 500 retry backoffs resulting in successful subsequent calls
    @Test
    void shouldTransparentlyRetryOnTransientServerErrorAndSucceed() {
        String retrySuccessResponse = "{\n" +
                "  \"choices\": [\n" +
                "    {\n" +
                "      \"message\": {\n" +
                "        \"content\": \"{\\\"suggestions\\\": []}\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        // Scenario-based stub: Call 1 fails (500), Call 2 succeeds (200)
        wireMock.stubFor(post(urlEqualTo("/"))
                .inScenario("Transient Failure Retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(serverError())
                .willSetStateTo("SecondAttempt"));

        wireMock.stubFor(post(urlEqualTo("/"))
                .inScenario("Transient Failure Retry")
                .whenScenarioStateIs("SecondAttempt")
                .willReturn(okJson(retrySuccessResponse)));

        ParsedSchema schema = new ParsedSchema(List.of(), List.of());
        List<RelationshipSuggestion> suggestions = gateway.inferRelationships(schema);

        assertNotNull(suggestions);
        assertTrue(suggestions.isEmpty());
    }

    // Scenario 3: Validate consecutive HTTP 500 errors tripping the circuit and bypassing network calls
    @Test
    void shouldOpenCircuitAndFailFastWhenConsequentialErrorsExceedThreshold() {
        // Stub all calls to return HTTP 500 Server Error
        wireMock.stubFor(post(urlEqualTo("/"))
                .willReturn(serverError()));

        ParsedSchema schema = new ParsedSchema(List.of(), List.of());

        // slidingWindowSize is 10. Execute 10 failed requests to trip the circuit
        for (int i = 0; i < 10; i++) {
            try {
                gateway.inferRelationships(schema);
            } catch (Exception ignored) {
                // Expect and ignore server errors during sliding window accumulation
            }
        }

        // The 11th request must bypass the network completely and fail fast with CallNotPermittedException
        assertThrows(CallNotPermittedException.class, () -> {
            gateway.inferRelationships(schema);
        });

        // Verify that only exactly 10 requests reached the WireMock server (the 11th was short-circuited)
        wireMock.verify(10, postRequestedFor(urlEqualTo("/")));
    }
}
