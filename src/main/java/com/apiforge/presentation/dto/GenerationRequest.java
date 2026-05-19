package com.apiforge.presentation.dto;

import com.apiforge.domain.model.GenerationOptions;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Presentation boundary DTO (Request) representing payload parameters for API code generation.
 * Employs bean validation annotations and normalizes optional boolean parameters via a compact constructor.
 */
public record GenerationRequest(
    @NotBlank(message = "SQL schema cannot be blank")
    @Size(max = 50000, message = "SQL schema cannot exceed 50000 characters")
    String sql,

    @NotBlank(message = "Package name cannot be blank")
    @Pattern(regexp = "^[a-z]+(\\.[a-z]+)*$", message = "Package name must be a valid lower-case Java package structure")
    String packageName,

    Boolean generateJwt,
    Boolean generatePagination,
    Boolean generateSoftDelete,
    Boolean enrichWithLlm
) {
    /**
     * Compact constructor to normalize and assign defaults to optional boolean attributes.
     */
    public GenerationRequest {
        if (generateJwt == null) {
            generateJwt = false;
        }
        if (generatePagination == null) {
            generatePagination = false;
        }
        if (generateSoftDelete == null) {
            generateSoftDelete = false;
        }
        if (enrichWithLlm == null) {
            enrichWithLlm = false;
        }
    }

    /**
     * Overloaded constructor to preserve backward compatibility for 5-parameter callers.
     */
    public GenerationRequest(String sql, String packageName, Boolean generateJwt, Boolean generatePagination, Boolean generateSoftDelete) {
        this(sql, packageName, generateJwt, generatePagination, generateSoftDelete, false);
    }

    /**
     * Maps this presentation DTO to the pure domain's {@link GenerationOptions} record.
     * 
     * @return Fully populated GenerationOptions.
     */
    public GenerationOptions toOptions() {
        return new GenerationOptions(
            packageName(),
            generateJwt(),
            generatePagination(),
            generateSoftDelete()
        );
    }
}
