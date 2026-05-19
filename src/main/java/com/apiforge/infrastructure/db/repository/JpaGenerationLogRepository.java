package com.apiforge.infrastructure.db.repository;

import com.apiforge.infrastructure.db.entity.GenerationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA Repository for managing the {@link GenerationLogEntity} database operations.
 */
@Repository
public interface JpaGenerationLogRepository extends JpaRepository<GenerationLogEntity, UUID> {
}
