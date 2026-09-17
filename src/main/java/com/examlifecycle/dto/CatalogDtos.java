package com.examlifecycle.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public class CatalogDtos {

    public record CourseRequest(
            @NotBlank(message = "code is required") String code,
            @NotBlank(message = "name is required") String name) {}

    public record SectionDto(String topic, Integer marks, Integer count) {}

    public record BlueprintRequest(
            @NotNull(message = "courseId is required") Long courseId,
            @NotBlank(message = "title is required") String title,
            @NotEmpty(message = "sections must not be empty") @Valid List<SectionDto> sections) {}

    public record BlueprintView(
            Long id, Long courseId, String title, List<SectionDto> sections, Long createdBy, Instant createdAt) {}

    public record QuestionDto(
            @NotBlank(message = "text is required") String text,
            String topic,
            @NotNull(message = "marks is required") Integer marks,
            String difficulty) {}

    public record QuestionImportRequest(@NotEmpty(message = "questions must not be empty") @Valid List<QuestionDto> questions) {}
}
