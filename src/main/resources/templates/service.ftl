package ${options.packageName}.application.service;

import ${options.packageName}.domain.entity.${table.className};
import ${options.packageName}.infrastructure.repository.${table.className}Repository;
import ${options.packageName}.presentation.dto.${table.className}Request;
import ${options.packageName}.presentation.dto.${table.className}Response;
import ${options.packageName}.presentation.mapper.${table.className}Mapper;
import ${options.packageName}.presentation.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.UUID;

/**
 * Service orchestrating application-level operations for ${table.className}.
 * Generated automatically by APIForge.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ${table.className}Service {

    private final ${table.className}Repository repository;
    private final ${table.className}Mapper mapper;

    /**
     * Retrieves all ${table.className} records with pagination support.
     */
    public Page<${table.className}Response> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toResponse);
    }

    /**
     * Retrieves a single ${table.className} record by ID.
     */
    public ${table.className}Response findById(UUID id) {
        ${table.className} entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("${table.className} with ID " + id + " not found"));
        return mapper.toResponse(entity);
    }

    /**
     * Creates a new ${table.className} record.
     */
    @Transactional
    public ${table.className}Response create(${table.className}Request request) {
        ${table.className} entity = mapper.toEntity(request);
        ${table.className} saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    /**
     * Updates an existing ${table.className} record.
     */
    @Transactional
    public ${table.className}Response update(UUID id, ${table.className}Request request) {
        ${table.className} existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("${table.className} with ID " + id + " not found"));
        
        ${table.className} updatedEntity = mapper.toEntity(request);
        updatedEntity.setId(existing.getId());
        
        <#if table.hasAuditFields>
        updatedEntity.setCreatedAt(existing.getCreatedAt());
        </#if>

        ${table.className} saved = repository.save(updatedEntity);
        return mapper.toResponse(saved);
    }

    /**
     * Deletes a ${table.className} record by ID.
     */
    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("${table.className} with ID " + id + " not found");
        }
        repository.deleteById(id);
    }
}
