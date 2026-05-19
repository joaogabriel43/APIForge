package com.apiforge.presentation.controller;

import com.apiforge.application.service.CodeGenerationService;
import com.apiforge.application.service.GenerationAuditService;
import com.apiforge.application.service.SqlSchemaParser;
import com.apiforge.domain.model.ParsedSchema;
import com.apiforge.infrastructure.service.ZipGeneratorService;
import com.apiforge.presentation.dto.GenerationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

/**
 * REST Controller responsible for orchestrating the synchronous API code generation pipeline
 * and returning the structured virtual file system packaged as an in-memory ZIP file.
 * 
 * <p>
 * Under strict Clean Architecture boundaries, this class contains zero business rules, SQL parsing,
 * or rendering formats. It acts strictly as an orchestrating boundary adapter.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/generate")
public class GenerationController {

    private final SqlSchemaParser sqlSchemaParser;
    private final CodeGenerationService codeGenerationService;
    private final ZipGeneratorService zipGeneratorService;
    private final GenerationAuditService generationAuditService;

    /**
     * Constructor injection for required orchestration services.
     */
    public GenerationController(
            SqlSchemaParser sqlSchemaParser,
            CodeGenerationService codeGenerationService,
            ZipGeneratorService zipGeneratorService,
            GenerationAuditService generationAuditService
    ) {
        this.sqlSchemaParser = sqlSchemaParser;
        this.codeGenerationService = codeGenerationService;
        this.zipGeneratorService = zipGeneratorService;
        this.generationAuditService = generationAuditService;
    }

    /**
     * POST endpoint executing the complete code generation pipeline.
     * Takes an SQL schema and custom parameters, returning a compressed ZIP containing the generated files.
     * 
     * @param request Validated GenerationRequest payload.
     * @return ResponseEntity holding the raw ZIP binary data and attachment headers.
     * @throws IOException If ZIP generation byte stream operations fail.
     */
    @PostMapping
    public ResponseEntity<byte[]> generate(@Valid @RequestBody GenerationRequest request) throws IOException {
        // 1. Invoke JSQLParser to build the parsed schema aggregate
        ParsedSchema schema = SqlSchemaParser.parse(request.sql());

        // 2. Render code templates for all layers, generating relative virtual files
        Map<String, String> generatedFiles = codeGenerationService.generate(schema, request.toOptions());

        // 3. Compress the virtual files map into a platform-agnostic in-memory ZIP byte array
        byte[] zipBytes = zipGeneratorService.generateZip(generatedFiles);

        // 4. Fire-and-forget background persistent auditing (totally isolated from client response outcomes)
        generationAuditService.audit(request.sql(), request.toOptions(), generatedFiles.size());

        // 5. Build and dispatch HTTP response with attachment configuration headers to trigger client download behavior
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/zip")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"api-forge-generated.zip\"")
                .body(zipBytes);
    }
}
