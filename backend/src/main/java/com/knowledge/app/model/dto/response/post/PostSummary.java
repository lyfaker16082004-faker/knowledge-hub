package com.knowledge.app.model.dto.response.post;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Thông tin tóm tắt một bài viết (hiển thị trong bảng tin).
 */
public record PostSummary(
        Long id,
        String type,            // QUESTION hoặc ARTICLE
        String title,
        String excerpt,         // Đoạn trích ngắn
        LocalDateTime publishedAt,
        Author author,
        Long categoryId,
        String category,
        List<String> tags,
        long likes,
        long comments,
        long views,
        boolean liked,          // Người xem đã thích chưa
        boolean saved,          // Người xem đã lưu chưa
        boolean mine) {         // Có phải bài của người xem không
}
