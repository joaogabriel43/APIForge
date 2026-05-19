package com.apiforge.presentation.controller;

import com.apiforge.application.service.CodeGenerationService;
import com.apiforge.application.service.GenerationAuditService;
import com.apiforge.application.service.SqlSchemaParser;
import com.apiforge.domain.model.GenerationOptions;
import com.apiforge.infrastructure.service.ZipGeneratorService;
import com.apiforge.presentation.dto.GenerationRequest;
import com.apiforge.presentation.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GenerationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SqlSchemaParser sqlSchemaParser;

    @Mock
    private CodeGenerationService codeGenerationService;

    @Mock
    private ZipGeneratorService zipGeneratorService;

    @Mock
    private GenerationAuditService generationAuditService;

    @InjectMocks
    private GenerationController generationController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        this.mockMvc = MockMvcBuilders.standaloneSetup(generationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldSuccessfullyProcessPipelineAndReturnZip() throws Exception {
        String sql = "CREATE TABLE users (id UUID PRIMARY KEY);";
        GenerationRequest payload = new GenerationRequest(
                sql,
                "com.example.api",
                true,
                false,
                true
        );
        String jsonPayload = objectMapper.writeValueAsString(payload);

        Map<String, String> dummyVirtualFiles = Map.of("src/main/java/User.java", "public class User {}");
        byte[] dummyZipBytes = new byte[]{80, 75, 3, 4, 10, 20, 30}; // standard ZIP header bytes

        // Stub services behavior
        when(codeGenerationService.generate(any(), any(GenerationOptions.class))).thenReturn(dummyVirtualFiles);
        when(zipGeneratorService.generateZip(dummyVirtualFiles)).thenReturn(dummyZipBytes);

        // Perform request and verify responses
        mockMvc.perform(post("/api/v1/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/zip"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"api-forge-generated.zip\""))
                .andExpect(content().bytes(dummyZipBytes));

        // Verify async persistent audit call parameters
        ArgumentCaptor<GenerationOptions> optionsCaptor = ArgumentCaptor.forClass(GenerationOptions.class);
        verify(generationAuditService, times(1)).audit(
                eq(sql),
                optionsCaptor.capture(),
                eq(1)
        );

        GenerationOptions capturedOptions = optionsCaptor.getValue();
        assertEquals("com.example.api", capturedOptions.packageName());
        assertEquals(true, capturedOptions.generateJwt());
        assertEquals(false, capturedOptions.generatePagination());
        assertEquals(true, capturedOptions.generateSoftDelete());
    }

    @Test
    void shouldFailValidationAndReturn400WhenPayloadIsInvalid() throws Exception {
        // Package name contains uppercase, which is invalid
        GenerationRequest invalidPayload = new GenerationRequest(
                "CREATE TABLE users (id UUID);",
                "com.Example.Api",
                true,
                true,
                true
        );
        String jsonPayload = objectMapper.writeValueAsString(invalidPayload);

        mockMvc.perform(post("/api/v1/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("https://apiforge.com/errors/validation-failed")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field", is("packageName")))
                .andExpect(jsonPath("$.errors[0].message", containsString("Package name must be a valid lower-case Java package structure")));

        // Verify that downstream generation services and audit logging were never invoked
        verifyNoInteractions(codeGenerationService);
        verifyNoInteractions(zipGeneratorService);
        verifyNoInteractions(generationAuditService);
    }

    @Test
    void shouldPropagateExceptionsToGlobalHandlerAndReturn500WhenZipFails() throws Exception {
        GenerationRequest payload = new GenerationRequest(
                "CREATE TABLE users (id UUID PRIMARY KEY);",
                "com.example.api",
                null, null, null
        );
        String jsonPayload = objectMapper.writeValueAsString(payload);

        // Stub services to raise IOException inside ZIP generator
        when(codeGenerationService.generate(any(), any(GenerationOptions.class))).thenReturn(Map.of("a.java", ""));
        when(zipGeneratorService.generateZip(any())).thenThrow(new IOException("Memory buffer overflow"));

        mockMvc.perform(post("/api/v1/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type", is("https://apiforge.com/errors/internal-server-error")))
                .andExpect(jsonPath("$.title", is("Internal Server Error")))
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.detail", is("An unexpected system error occurred. Please contact the administrator.")));
    }
}
