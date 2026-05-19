package ${options.packageName}.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;

/**
 * Entity class representing the database table "${table.name}".
 * Generated automatically by APIForge.
 */
@Entity
@Table(name = "${table.name}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ${table.className} {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

<#list table.columns as col>
  <#if !col.primaryKey && !col.foreignKey && col.name != "id" && col.name != "created_at" && col.name != "updated_at">
    @Column(name = "${col.name}"<#if !col.nullable>, nullable = false</#if><#if col.unique>, unique = true</#if>)
    private ${col.javaType} ${col.fieldName};

  </#if>
</#list>
<#list table.relationships as rel>
  <#if rel.type == "ManyToOne">
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "${rel.fkColumnName}"<#if !rel.nullable>, nullable = false</#if>)
    private ${rel.targetClassName} ${rel.fieldName};

  <#elseif rel.type == "OneToMany">
    @OneToMany(mappedBy = "${rel.mappedByFieldName}", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<${rel.targetClassName}> ${rel.fieldName} = new ArrayList<>();

  </#if>
</#list>
<#if table.hasAuditFields>
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
</#if>
}
