package com.example.reserve.dto;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Slf4j
public class DtoGenerator {
    // 프로젝트 루트 디렉토리를 기준으로 경로 설정
    private final String PROJECT_DIR = System.getProperty("user.dir");
    private String OUTPUT_DIR;
    private String DOCS_DIR;



    @Getter
    @AllArgsConstructor
    public class DtoTemplate {
        private final String className;
        private final Map<String, Class<?>> fields;
    }
    @PostConstruct
    public void init() {
        // 생성자 이후 초기화
        OUTPUT_DIR = PROJECT_DIR + "/src/main/java/generated/";
        DOCS_DIR = PROJECT_DIR + "/docs/dto/";

        // 초기 디렉토리 생성
        try {
            createDirectories();
        } catch (IOException e) {
            log.error("Failed to create initial directories", e);
        }
    }

    private void createDirectories() throws IOException {
        // 디렉토리 생성 시도
        File outputDir = new File(OUTPUT_DIR);
        File docsDir = new File(DOCS_DIR);

        if (!outputDir.exists()) {
            log.info("Creating output directory: {}", OUTPUT_DIR);
            if (!outputDir.mkdirs()) {
                log.error("Failed to create output directory");
            }
        }

        if (!docsDir.exists()) {
            log.info("Creating docs directory: {}", DOCS_DIR);
            if (!docsDir.mkdirs()) {
                log.error("Failed to create docs directory");
            }
        }
    }
    public DtoTemplate analyzeHashMap(String className, HashMap<String, Object> sample) {
        Map<String, Class<?>> fields = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : sample.entrySet()) {
            Class<?> type = inferType(entry.getValue());
            fields.put(entry.getKey(), type);
        }

        return new DtoTemplate(className, fields);
    }
    private Class<?> inferType(Object value) {
        if (value == null) return Object.class;
        if (value instanceof Integer) return Integer.class;
        if (value instanceof Long) return Long.class;
        if (value instanceof Double) return Double.class;
        if (value instanceof Boolean) return Boolean.class;
        return String.class;
    }

    public void generateDtoClass(DtoTemplate template) throws IOException {
        File file = new File(OUTPUT_DIR + template.getClassName() + ".java");
        log.info("Attempting to create DTO file at: {}", file.getAbsolutePath());

        // 부모 디렉토리가 없다면 생성
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            log.info("Creating parent directory: {}", parentDir.getAbsolutePath());
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
            }
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("package generated;");
            writer.println();
            writer.println("import lombok.Data;");
            writer.println("import lombok.Builder;");
            writer.println("import java.time.LocalDateTime;");
            writer.println();
            writer.println("/**");
            writer.println(" * Auto-generated DTO class");
            writer.println(" * Generated at: " + LocalDateTime.now());
            writer.println(" */");
            writer.println("@Data");
            writer.println("@Builder");
            writer.println("public class " + template.getClassName() + " {");

            for (Map.Entry<String, Class<?>> field : template.getFields().entrySet()) {
                writer.println("    private " + field.getValue().getSimpleName() + " " + field.getKey() + ";");
            }

            writer.println("}");

            log.info("Successfully generated DTO class at: {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write DTO file: {}", file.getAbsolutePath(), e);
            throw e;
        }
    }

    public void generateDtoDocumentation(DtoTemplate template) throws IOException {
        File file = new File(DOCS_DIR + template.getClassName() + ".md");
        log.info("Attempting to create documentation file at: {}", file.getAbsolutePath());

        // 부모 디렉토리가 없다면 생성
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            log.info("Creating parent directory: {}", parentDir.getAbsolutePath());
            if (!parentDir.mkdirs()) {
                throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
            }
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("# " + template.getClassName());
            writer.println();
            writer.println("Generated at: " + LocalDateTime.now());
            writer.println();
            writer.println("## Fields");
            writer.println("| Field Name | Type |");
            writer.println("|------------|------|");

            for (Map.Entry<String, Class<?>> field : template.getFields().entrySet()) {
                writer.println("| " + field.getKey() + " | " + field.getValue().getSimpleName() + " |");
            }

            log.info("Successfully generated documentation at: {}", file.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to write documentation file: {}", file.getAbsolutePath(), e);
            throw e;
        }
    }

    // 컨트롤러에서 파일 생성 위치를 확인할 수 있도록 메소드 추가
    public Map<String, String> getGeneratedFilePaths(String className) {
        return Map.of(
                "dtoPath", OUTPUT_DIR + className + ".java",
                "documentationPath", DOCS_DIR + className + ".md"
        );
    }
}