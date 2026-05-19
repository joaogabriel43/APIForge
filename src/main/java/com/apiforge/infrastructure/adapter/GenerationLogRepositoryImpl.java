package com.apiforge.infrastructure.adapter;

import com.apiforge.domain.model.GenerationLog;
import com.apiforge.domain.repository.GenerationLogRepository;
import com.apiforge.infrastructure.db.entity.GenerationLogEntity;
import com.apiforge.infrastructure.db.repository.JpaGenerationLogRepository;
import org.springframework.stereotype.Component;

/**
 * Concrete implementation adapter of {@link GenerationLogRepository} (Secondary Port).
 * Maps domain objects into JPA relational database entities to preserve pure domain isolation.
 */
@Component
public class GenerationLogRepositoryImpl implements GenerationLogRepository {

    private final JpaGenerationLogRepository jpaRepository;

    /**
     * Constructor injection for Spring Data Repository.
     * 
     * @param jpaRepository Concrete JPA handler.
     */
    public GenerationLogRepositoryImpl(JpaGenerationLogRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(GenerationLog generationLog) {
        if (generationLog == null) {
            return;
        }

        GenerationLogEntity entity = new GenerationLogEntity();
        entity.setId(generationLog.id());
        entity.setSqlHash(generationLog.sqlHash());
        entity.setPackageName(generationLog.packageName());
        entity.setOptions(generationLog.options());
        entity.setFileCount(generationLog.fileCount());
        entity.setCreatedAt(generationLog.createdAt());

        jpaRepository.save(entity);
    }
}
