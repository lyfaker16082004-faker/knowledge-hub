package com.knowledge.app.model.dto.response.admin;

import com.knowledge.app.model.dto.response.post.Author;
import java.time.LocalDateTime;

/** Một bình luận trong màn hình quản trị (kể cả bình luận bị ẩn / đã xoá). */
public record AdminCommentItem(
        Long id,
        Long postId,
        String postTitle,
        String postStatus,
        Long parentId,          // null = trả lời trực tiếp bài viết
        Author author,
        String content,
        String status,          // VISIBLE | HIDDEN (admin ẩn) | DELETED (tác giả xoá)
        LocalDateTime createdAt,
        LocalDateTime editedAt,
        long score,
        long replies) {
}
