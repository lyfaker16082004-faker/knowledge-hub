package com.knowledge.app.model.dto.request.document;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UploadDocumentRequest(
        @NotBlank @Size(max = 255) String title, @NotBlank @Size(max = 1500) String summary,
        @Size(max = 10000) String description, @NotNull @Positive Long categoryId,
        @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED") String level,
        @NotBlank @Pattern(regexp = "FREE|PAID") String accessType,
        @NotNull @DecimalMin("0") @DecimalMax("9999999999.99") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @Positive Long institutionId, @Positive Long facultyId, @Positive Long subjectId,
        @Size(max = 5) java.util.List<@NotBlank @Size(max = 80) String> tags) {
}
