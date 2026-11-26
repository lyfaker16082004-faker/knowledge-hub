package com.knowledge.app.model.dto.response.admin;

import com.knowledge.app.model.dto.response.post.Author;
import java.time.LocalDateTime;
import java.util.List;

/** Một bài viết Hỏi đáp trong màn hình quản trị (mọi trạng thái). */
public record AdminPostItem(
        Long id,
        String type,            // QUESTION | ARTICLE
        String title,
        String excerpt,
        String status,          // PUBLISHED | REJECTED (admin ẩn) | ARCHIVED (tác giả xoá)
        String visibility,
        Author author,
        String category,
        List<String> tags,
        LocalDateTime createdAt,
        LocalDateTime publishedAt,
        long likes,
        long comments,
        long views) {
}
