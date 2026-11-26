package com.knowledge.app.model.dto.response.document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Thông tin tóm tắt một tài liệu (hiển thị trong danh sách / thẻ tài liệu).
 */
public record DocumentSummary(
        Long id,
        String slug,
        String title,
        String summary,

        // Chủ đề
        Long categoryId,
        String category,

        // Giá và loại truy cập
        String accessType,      // FREE hoặc PAID
        BigDecimal price,
        String currency,

        String level,           // BEGINNER, INTERMEDIATE, ADVANCED
        String language,
        String coverStyle,
        boolean featured,
        LocalDateTime publishedAt,
        long downloads,         // Số lượt tải

        // Trường / khoa
        Long institutionId,
        String institutionName,
        Long facultyId,
        String facultyName,

        String fileFormat,      // VD: PDF, DOCX

        // Môn học và nhãn
        Long subjectId,
        String subjectName,
        List<String> tags) {
}
