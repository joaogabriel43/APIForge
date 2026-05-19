package com.apiforge.domain.repository;

import com.apiforge.domain.model.GenerationLog;

/**
 * Port interface for the generation audit log persistence.
 * Decouples the core application and domain from database specifics (e.g. JPA, Hibernate, PostgreSQL).
 */
public interface GenerationLogRepository {

    /**
     * Persists a completed {@link GenerationLog} domain entity.
     * 
     * @param generationLog The log entity to persist.
     */
    void save(GenerationLog generationLog);
}
