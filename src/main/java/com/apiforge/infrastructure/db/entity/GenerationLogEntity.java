package com.apiforge.infrastructure.db.entity;

import com.apiforge.domain.model.GenerationOptions;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA Entity representing the persistent database model of an API code generation audit log.
 * Strictly decoupled from the pure core business domain models.
 */
@Entity
@Table(name = "generation_log")
public class GenerationLogEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "sql_hash", nullable = false, length = 64)
    private String sqlHash;

    @Column(name = "package_name", nullable = false, length = 255)
    private String packageName;

    @Column(name = "options", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private GenerationOptions options;

    @Column(name = "file_count", nullable = false)
    private int fileCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Default constructor required by JPA.
     */
    public GenerationLogEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSqlHash() {
        return sqlHash;
    }

    public void setSqlHash(String sqlHash) {
        this.sqlHash = sqlHash;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public GenerationOptions getOptions() {
        return options;
    }

    public void setOptions(GenerationOptions options) {
        this.options = options;
    }

    public int getFileCount() {
        return fileCount;
    }

    public void setFileCount(int fileCount) {
        this.fileCount = fileCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
