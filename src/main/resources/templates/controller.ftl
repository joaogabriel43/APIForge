package ${options.packageName}.presentation.controller;

import ${options.packageName}.application.service.${table.className}Service;
import ${options.packageName}.presentation.dto.${table.className}Request;
import ${options.packageName}.presentation.dto.${table.className}Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.UUID;

/**
 * REST Controller exposing endpoints for ${table.className}.
 * Generated automatically by APIForge.
 */
@RestController
@RequestMapping("${table.restRoute}")
@RequiredArgsConstructor
public class ${table.className}Controller {

    private final ${table.className}Service service;

    /**
     * GET endpoint to retrieve a paginated list of all ${table.className} records.
     */
    @GetMapping
    public ResponseEntity<Page<${table.className}Response>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        Page<${table.className}Response> page = service.findAll(pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * GET endpoint to retrieve a single ${table.className} record by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<${table.className}Response> findById(@PathVariable UUID id) {
        ${table.className}Response response = service.findById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * POST endpoint to create a new ${table.className} record.
     */
    @PostMapping
    public ResponseEntity<${table.className}Response> create(@RequestBody @Valid ${table.className}Request request) {
        ${table.className}Response response = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT endpoint to update an existing ${table.className} record.
     */
    @PutMapping("/{id}")
    public ResponseEntity<${table.className}Response> update(
            @PathVariable UUID id, 
            @RequestBody @Valid ${table.className}Request request) {
        ${table.className}Response response = service.update(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE endpoint to delete a ${table.className} record by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
