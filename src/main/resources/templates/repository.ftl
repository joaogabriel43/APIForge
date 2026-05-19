package ${options.packageName}.infrastructure.repository;

import ${options.packageName}.domain.entity.${table.className};
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

/**
 * Spring Data JPA Repository for ${table.className} Entity.
 * Generated automatically by APIForge.
 */
@Repository
public interface ${table.className}Repository extends JpaRepository<${table.className}, UUID> {
}
