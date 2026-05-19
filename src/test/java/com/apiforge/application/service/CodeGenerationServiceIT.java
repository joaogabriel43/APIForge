package com.apiforge.application.service;

import com.apiforge.domain.model.GenerationOptions;
import com.apiforge.domain.model.ParsedSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CodeGenerationServiceIT {

    private final CodeGenerationService service = new CodeGenerationService();

    @Test
    void testScenario1_SimpleSchema(@TempDir Path tempDir) throws IOException {
        String sql = """
            CREATE TABLE users (
                id UUID PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                email VARCHAR(255) UNIQUE,
                created_at TIMESTAMP,
                updated_at TIMESTAMP
            );
            """;
        ParsedSchema schema = SqlSchemaParser.parse(sql);
        GenerationOptions options = new GenerationOptions("com.example.simpleapp", false, false, false);

        Map<String, String> files = service.generate(schema, options);

        // Verify basic structures are generated
        assertNotNull(files);
        assertFalse(files.isEmpty());
        
        assertTrue(files.containsKey("src/main/java/com/example/simpleapp/domain/entity/Users.java"));
        assertTrue(files.containsKey("src/main/java/com/example/simpleapp/infrastructure/repository/UsersRepository.java"));
        assertTrue(files.containsKey("src/main/java/com/example/simpleapp/application/service/UsersService.java"));
        assertTrue(files.containsKey("src/main/java/com/example/simpleapp/presentation/controller/UsersController.java"));
        assertTrue(files.containsKey("src/main/java/com/example/simpleapp/presentation/dto/UsersRequest.java"));
        assertTrue(files.containsKey("src/main/java/com/example/simpleapp/presentation/dto/UsersResponse.java"));
        assertTrue(files.containsKey("src/main/java/com/example/simpleapp/presentation/mapper/UsersMapper.java"));
        assertTrue(files.containsKey("src/main/resources/db/migration/V1__create_users_table.sql"));
        assertTrue(files.containsKey("docker-compose.yml"));
        assertTrue(files.containsKey("src/main/resources/application.properties"));
        assertTrue(files.containsKey("pom.xml"));

        // Compile and validate syntactical correctness via JavaCompiler API
        compileAndValidate(files, options, tempDir);
    }

    @Test
    void testScenario2_SchemaWithForeignKey(@TempDir Path tempDir) throws IOException {
        String sql = """
            CREATE TABLE users (
                id UUID PRIMARY KEY,
                name VARCHAR(100) NOT NULL
            );
            CREATE TABLE posts (
                id UUID PRIMARY KEY,
                title VARCHAR(200) NOT NULL,
                content TEXT,
                user_id UUID NOT NULL REFERENCES users(id)
            );
            """;
        ParsedSchema schema = SqlSchemaParser.parse(sql);
        GenerationOptions options = new GenerationOptions("com.example.relationalapp", false, false, false);

        Map<String, String> files = service.generate(schema, options);

        // Verify relational components are generated
        assertTrue(files.containsKey("src/main/java/com/example/relationalapp/domain/entity/Users.java"));
        assertTrue(files.containsKey("src/main/java/com/example/relationalapp/domain/entity/Posts.java"));
        assertTrue(files.containsKey("src/main/java/com/example/relationalapp/presentation/mapper/PostsMapper.java"));
        assertTrue(files.containsKey("src/main/resources/db/migration/V1__create_posts_table.sql"));

        // Compile and validate relational mappings
        compileAndValidate(files, options, tempDir);
    }

    @Test
    void testScenario3_GenerationOptions() {
        String sql = """
            CREATE TABLE products (
                id UUID PRIMARY KEY,
                name VARCHAR(100) NOT NULL
            );
            """;
        ParsedSchema schema = SqlSchemaParser.parse(sql);
        
        // Options with JWT and Pagination enabled
        GenerationOptions options = new GenerationOptions("com.example.securedapp", true, true, false);

        Map<String, String> files = service.generate(schema, options);

        // Verify pom.xml includes conditional spring-boot-starter-security
        String pomContent = files.get("pom.xml");
        assertNotNull(pomContent);
        assertTrue(pomContent.contains("spring-boot-starter-security"), "POM should contain Spring Security");
        assertTrue(pomContent.contains("jjwt-api"), "POM should contain JJWT dependencies");

        // Verify service uses Pageable
        String serviceContent = files.get("src/main/java/com/example/securedapp/application/service/ProductsService.java");
        assertNotNull(serviceContent);
        assertTrue(serviceContent.contains("Page<ProductsResponse> findAll(Pageable pageable)"), "Service should use Spring Data Page");
    }

    private void compileAndValidate(Map<String, String> generatedFiles, GenerationOptions options, Path tempDir) throws IOException {
        // 1. Create directory structure for compilation stubs
        createStub(tempDir, "lombok", "Getter", """
            package lombok;
            import java.lang.annotation.*;
            @Target({ElementType.TYPE, ElementType.FIELD})
            @Retention(RetentionPolicy.SOURCE)
            public @interface Getter {}
            """);
        createStub(tempDir, "lombok", "Setter", """
            package lombok;
            import java.lang.annotation.*;
            @Target({ElementType.TYPE, ElementType.FIELD})
            @Retention(RetentionPolicy.SOURCE)
            public @interface Setter {}
            """);
        createStub(tempDir, "lombok", "NoArgsConstructor", """
            package lombok;
            import java.lang.annotation.*;
            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.SOURCE)
            public @interface NoArgsConstructor {}
            """);
        createStub(tempDir, "lombok", "AllArgsConstructor", """
            package lombok;
            import java.lang.annotation.*;
            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.SOURCE)
            public @interface AllArgsConstructor {}
            """);
        createStub(tempDir, "lombok", "Builder", """
            package lombok;
            import java.lang.annotation.*;
            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.SOURCE)
            public @interface Builder {
                @Target(ElementType.FIELD)
                @Retention(RetentionPolicy.SOURCE)
                public @interface Default {}
            }
            """);
        createStub(tempDir, "lombok", "RequiredArgsConstructor", """
            package lombok;
            import java.lang.annotation.*;
            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.SOURCE)
            public @interface RequiredArgsConstructor {}
            """);

        // MapStruct Stubs
        createStub(tempDir, "org/mapstruct", "Mapper", """
            package org.mapstruct;
            import java.lang.annotation.*;
            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.CLASS)
            public @interface Mapper {
                String componentModel() default "";
            }
            """);
        createStub(tempDir, "org/mapstruct", "Mapping", """
            package org.mapstruct;
            import java.lang.annotation.*;
            @Target(ElementType.METHOD)
            @Retention(RetentionPolicy.CLASS)
            @Repeatable(Mappings.class)
            public @interface Mapping {
                String target() default "";
                String source() default "";
            }
            """);
        createStub(tempDir, "org/mapstruct", "Mappings", """
            package org.mapstruct;
            import java.lang.annotation.*;
            @Target(ElementType.METHOD)
            @Retention(RetentionPolicy.CLASS)
            public @interface Mappings {
                Mapping[] value();
            }
            """);

        // Jakarta Validation Stubs
        createStub(tempDir, "jakarta/validation/constraints", "NotNull", """
            package jakarta.validation.constraints;
            import java.lang.annotation.*;
            @Target({ElementType.METHOD, ElementType.FIELD})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface NotNull {
                String message() default "";
            }
            """);
        createStub(tempDir, "jakarta/validation/constraints", "NotBlank", """
            package jakarta.validation.constraints;
            import java.lang.annotation.*;
            @Target({ElementType.METHOD, ElementType.FIELD})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface NotBlank {
                String message() default "";
            }
            """);
        createStub(tempDir, "jakarta/validation/constraints", "Size", """
            package jakarta.validation.constraints;
            import java.lang.annotation.*;
            @Target({ElementType.METHOD, ElementType.FIELD})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Size {
                int max() default 2147483647;
                String message() default "";
            }
            """);
        createStub(tempDir, "jakarta/validation", "Valid", """
            package jakarta.validation;
            import java.lang.annotation.*;
            @Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface Valid {}
            """);

        // Spring PageableDefault Stub
        createStub(tempDir, "org/springframework/data/web", "PageableDefault", """
            package org.springframework.data.web;
            import java.lang.annotation.*;
            @Target({ElementType.PARAMETER})
            @Retention(RetentionPolicy.RUNTIME)
            public @interface PageableDefault {
                int size() default 10;
            }
            """);

        // Custom ResourceNotFoundException Stub
        String excPkg = options.packageName().replace('.', '/') + "/presentation/exception";
        createStub(tempDir, excPkg, "ResourceNotFoundException", String.format("""
            package %s.presentation.exception;
            public class ResourceNotFoundException extends RuntimeException {
                public ResourceNotFoundException(String message) {
                    super(message);
                }
            }
            """, options.packageName()));

        // 2. Write generated Java source files into the temp path
        List<File> sourceFiles = new ArrayList<>();
        for (Map.Entry<String, String> entry : generatedFiles.entrySet()) {
            if (entry.getKey().endsWith(".java")) {
                String sourceCode = entry.getValue();
                
                // Preprocess: Inject getters and setters if the class is an @Entity
                if (sourceCode.contains("@Entity")) {
                    sourceCode = injectGettersAndSetters(sourceCode);
                }
                
                // Preprocess: Inject constructor if the class has final fields and uses @RequiredArgsConstructor
                if (sourceCode.contains("private final ") || sourceCode.contains("@RequiredArgsConstructor")) {
                    sourceCode = injectRequiredArgsConstructor(sourceCode);
                }
                
                Path filePath = tempDir.resolve(entry.getKey());
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, sourceCode);
                sourceFiles.add(filePath.toFile());
            }
        }

        // Add stub files recursively
        Files.walk(tempDir)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p -> {
                    File file = p.toFile();
                    if (!sourceFiles.contains(file)) {
                        sourceFiles.add(file);
                    }
                });

        // 3. Compile programmatically using javax.tools.JavaCompiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "System Java compiler is not available. Ensure you are running with a JDK, not a JRE.");

        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(sourceFiles);

        // Put the active system classpath onto the compiler's classpath so Spring/JPA interfaces resolve
        List<String> compilerOptions = Arrays.asList(
                "-classpath", System.getProperty("java.class.path"),
                "-d", tempDir.toAbsolutePath().toString()
        );

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCompiler.CompilationTask task = compiler.getTask(
                null, fileManager, diagnostics, compilerOptions, null, compilationUnits
        );

        boolean success = task.call();
        fileManager.close();

        // 4. Assert success or print structured diagnostics
        if (!success) {
            String errorMsg = diagnostics.getDiagnostics().stream()
                    .map(Diagnostic::toString)
                    .collect(Collectors.joining("\n"));
            fail("Generated Java classes failed compilation validation:\n" + errorMsg);
        }
    }

    private static String injectGettersAndSetters(String sourceCode) {
        java.util.regex.Pattern fieldPattern = java.util.regex.Pattern.compile(
            "private\\s+([A-Za-z0-9_<>?.]+)\\s+([A-Za-z0-9_]+)\\s*(?:=[^;]+)?;"
        );
        java.util.regex.Matcher matcher = fieldPattern.matcher(sourceCode);
        StringBuilder gettersSetters = new StringBuilder("\n");
        while (matcher.find()) {
            String type = matcher.group(1);
            String name = matcher.group(2);
            String capName = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            
            gettersSetters.append("    public ").append(type).append(" get").append(capName).append("() { return this.").append(name).append("; }\n");
            gettersSetters.append("    public void set").append(capName).append("(").append(type).append(" ").append(name).append(") { this.").append(name).append(" = ").append(name).append("; }\n");
        }
        
        int lastBrace = sourceCode.lastIndexOf('}');
        if (lastBrace != -1) {
            return sourceCode.substring(0, lastBrace) + gettersSetters.toString() + "\n}";
        }
        return sourceCode;
    }

    private static String injectRequiredArgsConstructor(String sourceCode) {
        java.util.regex.Pattern finalFieldPattern = java.util.regex.Pattern.compile(
            "private\\s+final\\s+([A-Za-z0-9_<>?.]+)\\s+([A-Za-z0-9_]+)\\s*;"
        );
        java.util.regex.Matcher matcher = finalFieldPattern.matcher(sourceCode);
        List<String> types = new ArrayList<>();
        List<String> names = new ArrayList<>();
        while (matcher.find()) {
            types.add(matcher.group(1));
            names.add(matcher.group(2));
        }

        if (!names.isEmpty()) {
            java.util.regex.Pattern classPattern = java.util.regex.Pattern.compile("public\\s+class\\s+([A-Za-z0-9_]+)");
            java.util.regex.Matcher classMatcher = classPattern.matcher(sourceCode);
            if (classMatcher.find()) {
                String className = classMatcher.group(1);
                StringBuilder constructor = new StringBuilder("\n    public ").append(className).append("(");
                for (int i = 0; i < names.size(); i++) {
                    if (i > 0) constructor.append(", ");
                    constructor.append(types.get(i)).append(" ").append(names.get(i));
                }
                constructor.append(") {\n");
                for (String name : names) {
                    constructor.append("        this.").append(name).append(" = ").append(name).append(";\n");
                }
                constructor.append("    }\n");
                
                int lastBrace = sourceCode.lastIndexOf('}');
                if (lastBrace != -1) {
                    return sourceCode.substring(0, lastBrace) + constructor.toString() + "\n}";
                }
            }
        }
        return sourceCode;
    }

    private void createStub(Path tempDir, String packagePath, String className, String code) throws IOException {
        Path path = tempDir.resolve("stubs/" + packagePath + "/" + className + ".java");
        Files.createDirectories(path.getParent());
        Files.writeString(path, code);
    }
}
