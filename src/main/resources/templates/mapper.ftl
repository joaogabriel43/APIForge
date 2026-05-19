package ${options.packageName}.presentation.mapper;

import ${options.packageName}.domain.entity.${table.className};
import ${options.packageName}.presentation.dto.${table.className}Request;
import ${options.packageName}.presentation.dto.${table.className}Response;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

/**
 * MapStruct Mapper interface for ${table.className} Entity and DTO conversions.
 * Generated automatically by APIForge.
 */
@Mapper(componentModel = "spring")
public interface ${table.className}Mapper {

<#list table.relationships as rel>
  <#if rel.type == "ManyToOne">
    @Mapping(target = "${rel.fieldName}.id", source = "${rel.fieldName}Id")
  </#if>
</#list>
    ${table.className} toEntity(${table.className}Request request);

<#list table.relationships as rel>
  <#if rel.type == "ManyToOne">
    @Mapping(target = "${rel.fieldName}Id", source = "${rel.fieldName}.id")
  </#if>
</#list>
    ${table.className}Response toResponse(${table.className} entity);

    List<${table.className}Response> toResponseList(List<${table.className}> entities);
}
