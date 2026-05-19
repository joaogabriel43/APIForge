package com.apiforge.presentation.controller;

import com.apiforge.application.service.CodeGenerationService;
import com.apiforge.application.service.GenerationAuditService;
import com.apiforge.application.service.SchemaEnrichmentService;
import com.apiforge.application.service.SqlSchemaParser;
import com.apiforge.domain.model.EnrichedSchema;
import com.apiforge.domain.model.GenerationOptions;
import com.apiforge.domain.model.ParsedSchema;
import com.apiforge.infrastructure.service.ZipGeneratorService;
import com.apiforge.presentation.dto.GenerationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller responsible for orchestrating the synchronous API code generation pipeline,
 * returning in-memory ZIP archives, and providing async Server-Sent Events (SSE) previews in real-time.
 * 
 * <p>
 * Under strict Clean Architecture boundaries, this class contains zero business rules, SQL parsing,
 * or rendering formats. It acts strictly as an orchestrating boundary adapter.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/generate")
public class GenerationController {

    private static final Logger log = LoggerFactory.getLogger(GenerationController.class);

    private final SqlSchemaParser sqlSchemaParser;
    private final SchemaEnrichmentService schemaEnrichmentService;
    private final CodeGenerationService codeGenerationService;
    private final ZipGeneratorService zipGeneratorService;
    private final GenerationAuditService generationAuditService;
    private final ObjectMapper objectMapper;

    /**
     * Constructor injection for required orchestration services and Jackson parser.
     */
    public GenerationController(
            SqlSchemaParser sqlSchemaParser,
            SchemaEnrichmentService schemaEnrichmentService,
            CodeGenerationService codeGenerationService,
            ZipGeneratorService zipGeneratorService,
            GenerationAuditService generationAuditService,
            ObjectMapper objectMapper
    ) {
        this.sqlSchemaParser = sqlSchemaParser;
        this.schemaEnrichmentService = schemaEnrichmentService;
        this.codeGenerationService = codeGenerationService;
        this.zipGeneratorService = zipGeneratorService;
        this.generationAuditService = generationAuditService;
        this.objectMapper = objectMapper;
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

        // 1b. Enrich schema via LLM (with silent fallback try-catch inside the service)
        EnrichedSchema enrichedSchema = schemaEnrichmentService.enrich(schema, request.enrichWithLlm());

        // 2. Render code templates for all layers, generating relative virtual files
        Map<String, String> generatedFiles = codeGenerationService.generate(enrichedSchema, request.toOptions());

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

    /**
     * GET endpoint providing Server-Sent Events (SSE) preview of generated code files in real-time.
     * Offloads rendering logic into an asynchronous background executor thread, preventing Servlet thread blockages.
     * Performs direct initial validations before initiating the SSE stream.
     *
     * @param sql                 The raw SQL schema string query parameter (required).
     * @param packageName         Target base Java package name query parameter (required).
     * @param generateJwt         Flag enabling Spring Security JWT components (optional, defaults to false).
     * @param generatePagination  Flag enabling controller Page/Pageable integration (optional, defaults to false).
     * @param generateSoftDelete  Flag enabling SQL logical soft delete mechanisms (optional, defaults to false).
     * @param enrichWithLlm       Flag enabling LLM-based enrichment (optional, defaults to false).
     * @return Fully configured SseEmitter instance.
     */
    @GetMapping("/preview")
    public SseEmitter preview(
            @RequestParam("sql") String sql,
            @RequestParam("packageName") String packageName,
            @RequestParam(value = "generateJwt", required = false, defaultValue = "false") boolean generateJwt,
            @RequestParam(value = "generatePagination", required = false, defaultValue = "false") boolean generatePagination,
            @RequestParam(value = "generateSoftDelete", required = false, defaultValue = "false") boolean generateSoftDelete,
            @RequestParam(value = "enrichWithLlm", required = false, defaultValue = "false") boolean enrichWithLlm
    ) {
        // Perform GET parameter validations matching structural DTO annotations
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL schema cannot be blank");
        }
        if (packageName == null || packageName.isBlank()) {
            throw new IllegalArgumentException("Package name cannot be blank");
        }
        if (!packageName.matches("^[a-z]+(\\.[a-z]+)*$")) {
            throw new IllegalArgumentException("Package name must be a valid lower-case Java package structure");
        }

        // Configure standard SSE emitter with 60 seconds (60000ms) timeout bound
        SseEmitter emitter = new SseEmitter(60000L);

        // Decouple Servlet container HTTP threads completely from rendering operations
        CompletableFuture.runAsync(() -> {
            try {
                // Phase 1: progress - parsing schema
                if (!sendProgressEvent(emitter, "parsing", "Parsing SQL schema...")) {
                    return;
                }

                ParsedSchema schema = SqlSchemaParser.parse(sql);
                EnrichedSchema enrichedSchema = schemaEnrichmentService.enrich(schema, enrichWithLlm);
                GenerationOptions options = new GenerationOptions(packageName, generateJwt, generatePagination, generateSoftDelete);
                
                // Phase 2: rendering templates
                Map<String, String> generatedFiles = codeGenerationService.generate(enrichedSchema, options);

                // Phase 3: streaming generated virtual files with artificially injected delay
                for (Map.Entry<String, String> entry : generatedFiles.entrySet()) {
                    // Artificial cadence delay
                    Thread.sleep(50);
                    
                    if (!sendFileEvent(emitter, entry.getKey(), entry.getValue())) {
                        log.info("Preview streaming stopped: client aborted connection.");
                        return; // Break asynchronous execution thread immediately
                    }
                }

                // Phase 4: complete preview transmission
                sendCompleteEvent(emitter, "Generation complete", generatedFiles.size());
                emitter.complete();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Asynchronous preview code generation thread interrupted.");
            } catch (Exception e) {
                log.error("Internal preview generation pipeline failure.", e);
                sendErrorEvent(emitter, "Internal error: " + e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private boolean sendProgressEvent(SseEmitter emitter, String step, String message) {
        try {
            Map<String, String> payload = Map.of("step", step, "message", message);
            String json = objectMapper.writeValueAsString(payload);
            emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(json, MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException e) {
            handleClientAbort(e);
            return false;
        }
    }

    private boolean sendFileEvent(SseEmitter emitter, String path, String content) {
        try {
            Map<String, String> payload = Map.of("path", path, "content", content);
            String json = objectMapper.writeValueAsString(payload);
            emitter.send(SseEmitter.event()
                    .name("file")
                    .data(json, MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException e) {
            handleClientAbort(e);
            return false;
        }
    }

    private boolean sendCompleteEvent(SseEmitter emitter, String message, int fileCount) {
        try {
            Map<String, Object> payload = Map.of("message", message, "fileCount", fileCount);
            String json = objectMapper.writeValueAsString(payload);
            emitter.send(SseEmitter.event()
                    .name("complete")
                    .data(json, MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException e) {
            handleClientAbort(e);
            return false;
        }
    }

    private boolean sendErrorEvent(SseEmitter emitter, String message) {
        try {
            Map<String, String> payload = Map.of("message", message);
            String json = objectMapper.writeValueAsString(payload);
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(json, MediaType.APPLICATION_JSON));
            return true;
        } catch (IOException e) {
            handleClientAbort(e);
            return false;
        }
    }

    private void handleClientAbort(IOException e) {
        // Capture premature socket terminations and Client Abort clean-ups, avoiding server log pollutions
        log.info("Client terminated SSE preview socket session prematurely: {}", e.getMessage());
    }
}
