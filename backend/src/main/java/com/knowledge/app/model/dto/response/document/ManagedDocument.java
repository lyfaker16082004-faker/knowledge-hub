package com.knowledge.app.model.dto.response.document;

import java.time.LocalDateTime;

/**
 * Thông tin tài liệu trong trang quản lý (của tác giả hoặc admin).
 */
public record ManagedDocument(
        DocumentSummary document,
        String description,
        Long authorId,
        String author,
        String status,
        String reviewNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long version) {
}
