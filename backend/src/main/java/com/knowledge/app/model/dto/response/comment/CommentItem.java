package com.knowledge.app.model.dto.response.comment;

import java.time.LocalDateTime;

import com.knowledge.app.model.dto.response.post.Author;

/**
 * Một bình luận. Bình luận đã xoá thì author và content là null.
 */
public record CommentItem(
        Long id,
        Long parentId,          // null = bình luận gốc
        Author author,
        String content,
        boolean deleted,
        LocalDateTime createdAt,
        LocalDateTime editedAt,
        long score,             // Tổng điểm bình chọn
        int myVote,             // Bình chọn của người xem: 1, 0 hoặc -1
        boolean mine,
        boolean canDelete) {
}
