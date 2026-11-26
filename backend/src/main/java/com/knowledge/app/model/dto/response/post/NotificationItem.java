package com.knowledge.app.model.dto.response.post;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Một thông báo: ai đó bình luận bài viết của tôi, hoặc trả lời bình luận của tôi.
 */
public record NotificationItem(
        Long commentId,
        Long postId,
        String postTitle,
        String kind,            // COMMENT = bình luận bài của tôi, REPLY = trả lời bình luận của tôi
        Author actor,           // Người bình luận
        String excerpt,         // Đoạn đầu nội dung bình luận
        LocalDateTime createdAt,
        boolean unread) {

    /** Danh sách thông báo kèm số chưa đọc. */
    public record Page(long unread, List<NotificationItem> items) {
    }
}
