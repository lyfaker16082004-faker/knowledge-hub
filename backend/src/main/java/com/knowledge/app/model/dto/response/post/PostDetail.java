package com.knowledge.app.model.dto.response.post;

import java.time.LocalDateTime;

/**
 * Chi tiết một bài viết (kèm toàn bộ nội dung).
 */
public record PostDetail(
        PostSummary post,
        String content,
        LocalDateTime updatedAt,
        Long version,
        boolean canModerate) {
}
