package com.apiforge.presentation.controller;

import com.apiforge.application.service.CodeGenerationService;
import com.apiforge.application.service.GenerationAuditService;
import com.apiforge.application.service.SchemaEnrichmentService;
import com.apiforge.application.service.SqlSchemaParser;
import com.apiforge.domain.model.EnrichedSchema;
import com.apiforge.domain.model.GenerationOptions;
import com.apiforge.presentation.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GenerationControllerSseTest {

    private MockMvc mockMvc;

    @Mock
    private SqlSchemaParser sqlSchemaParser;

    @Mock
    private SchemaEnrichmentService schemaEnrichmentService;

    @Mock
    private CodeGenerationService codeGenerationService;

    @Mock
    private GenerationAuditService generationAuditService;

    private GenerationController generationController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup default transparent enrichment mock behavior
        when(schemaEnrichmentService.enrich(any(), anyBoolean()))
                .thenAnswer(invocation -> new EnrichedSchema(invocation.getArgument(0), List.of(), Map.of(), Map.of()));

        this.generationController = new GenerationController(
                sqlSchemaParser,
                schemaEnrichmentService,
                codeGenerationService,
                null, // zipGeneratorService is not used in SSE preview
                generationAuditService,
                objectMapper
        );
        this.mockMvc = MockMvcBuilders.standaloneSetup(generationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldSuccessfullyStreamSseEvents() throws Exception {
        String sql = "CREATE TABLE users (id UUID PRIMARY KEY);";
        String packageName = "com.example.api";

        Map<String, String> dummyVirtualFiles = Map.of(
                "src/main/java/User.java", "public class User {}"
        );

        when(codeGenerationService.generate(any(), any(GenerationOptions.class))).thenReturn(dummyVirtualFiles);

        MvcResult mvcResult = mockMvc.perform(get("/api/v1/generate/preview")
                        .param("sql", sql)
                        .param("packageName", packageName)
                        .param("generateJwt", "true")
                        .param("generatePagination", "false")
                        .param("generateSoftDelete", "true"))
                .andExpect(request().asyncStarted())
                .andReturn();

        // Dispatch async request to execute background CompletableFuture thread
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(content().string(containsString("event:progress")))
                .andExpect(content().string(containsString("Parsing SQL schema...")))
                .andExpect(content().string(containsString("event:file")))
                .andExpect(content().string(containsString("User.java")))
                .andExpect(content().string(containsString("event:complete")))
                .andExpect(content().string(containsString("Generation complete")));

        // Verify auditing is NOT executed on the preview route
        verifyNoInteractions(generationAuditService);
    }

    @Test
    void shouldFailValidationAndReturn400WhenSseParametersAreBlank() throws Exception {
        mockMvc.perform(get("/api/v1/generate/preview")
                        .param("sql", "")
                        .param("packageName", "com.example.api"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("https://apiforge.com/errors/validation-failed")))
                .andExpect(jsonPath("$.detail", is("SQL schema cannot be blank")));
    }

    @Test
    void shouldFailValidationAndReturn400WhenSsePackageIsMalformed() throws Exception {
        mockMvc.perform(get("/api/v1/generate/preview")
                        .param("sql", "CREATE TABLE users (id UUID PRIMARY KEY);")
                        .param("packageName", "com.Example.Api"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("https://apiforge.com/errors/validation-failed")))
                .andExpect(jsonPath("$.detail", is("Package name must be a valid lower-case Java package structure")));
    }
}
