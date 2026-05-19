package com.apiforge.infrastructure.adapter;

import com.apiforge.domain.model.ParsedSchema;
import com.apiforge.domain.model.RelationshipSuggestion;
import com.apiforge.domain.repository.LlmGateway;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Concrete implementation of the {@link LlmGateway} port in the infrastructure layer.
 * Utilizes the Spring native {@link RestClient} to interface with the OpenAI chat completions endpoint,
 * guarded by Resilience4j aspects to ensure reliability under transient outages or latency spikes.
 */
@Component
@CircuitBreaker(name = "openai")
@Retry(name = "openai")
public class OpenAiLlmGateway implements LlmGateway {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAiLlmGateway(
            @Value("${openai.api.url}") String apiUrl,
            @Value("${openai.api.key:}") String apiKey,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;

        // Configure connection timeouts directly at HTTP client level to satisfy TimeLimiter thresholds safely
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000); // 10 seconds connection timeout
        requestFactory.setReadTimeout(25000);    // 25 seconds read timeout

        this.restClient = RestClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public List<RelationshipSuggestion> inferRelationships(ParsedSchema schema) {
        String schemaJson;
        try {
            schemaJson = objectMapper.writeValueAsString(schema);
        } catch (Exception e) {
            schemaJson = schema.toString();
        }

        String systemPrompt = "Given this SQL schema: " + schemaJson + " Identify implicit foreign key relationships. Your output must be a valid JSON object matching this schema: { \"suggestions\": [ { \"fromTable\": \"string\", \"fromColumn\": \"string\", \"toTable\": \"string\", \"toColumn\": \"string\", \"type\": \"string\" } ] }";
        String responseContent = callOpenAi(systemPrompt, "Identify implicit relationships in this schema.");

        try {
            JsonNode root = objectMapper.readTree(responseContent);
            JsonNode suggestionsNode = root.get("suggestions");
            if (suggestionsNode != null && suggestionsNode.isArray()) {
                return objectMapper.readValue(suggestionsNode.toString(), new TypeReference<List<RelationshipSuggestion>>() {});
            }
            return objectMapper.readValue(responseContent, new TypeReference<List<RelationshipSuggestion>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LLM relationship suggestions", e);
        }
    }

    @Override
    public Map<String, String> suggestDomainNames(List<String> technicalNames) {
        String namesJson;
        try {
            namesJson = objectMapper.writeValueAsString(technicalNames);
        } catch (Exception e) {
            namesJson = technicalNames.toString();
        }

        String systemPrompt = "Given these technical identifiers: " + namesJson + " Suggest cleaner domain-oriented names. Your output must be a valid JSON object mapping each technical name key to its suggested domain name value (e.g., { \"tech_name\": \"DomainName\" }).";
        String responseContent = callOpenAi(systemPrompt, "Suggest cleaner domain-oriented names for the provided technical identifiers.");

        try {
            return objectMapper.readValue(responseContent, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LLM domain names suggestions", e);
        }
    }

    @Override
    public Map<String, String> generateJavadoc(List<String> classContexts) {
        String signaturesJson;
        try {
            signaturesJson = objectMapper.writeValueAsString(classContexts);
        } catch (Exception e) {
            signaturesJson = classContexts.toString();
        }

        String systemPrompt = "Given these Java class/method signatures: " + signaturesJson + " Generate concise Javadoc. Your output must be a valid JSON object mapping each signature/context key to its generated concise Javadoc comment block value.";
        String responseContent = callOpenAi(systemPrompt, "Generate concise Javadocs for the provided contexts.");

        try {
            return objectMapper.readValue(responseContent, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LLM Javadoc responses", e);
        }
    }

    private String callOpenAi(String systemPrompt, String userPrompt) {
        ChatCompletionRequest request = new ChatCompletionRequest(
                "gpt-4o-mini",
                List.of(
                        new Message("system", systemPrompt),
                        new Message("user", userPrompt)
                ),
                new ResponseFormat("json_object")
        );

        ChatCompletionResponse response = restClient.post()
                .body(request)
                .retrieve()
                .body(ChatCompletionResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new RuntimeException("Empty completion response received from OpenAI API");
        }

        return response.choices().get(0).message().content();
    }

    // OpenAI Chat Completions DTO mappings
    private record ChatCompletionRequest(
            String model,
            List<Message> messages,
            ResponseFormat response_format
    ) {}

    private record Message(
            String role,
            String content
    ) {}

    private record ResponseFormat(
            String type
    ) {}

    private record ChatCompletionResponse(
            List<Choice> choices
    ) {}

    private record Choice(
            Message message
    ) {}
}
