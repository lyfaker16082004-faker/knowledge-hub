package com.knowledge.app.service.post;

import com.knowledge.app.model.dto.response.post.NotificationItem;

/**
 * Thông báo bình luận trên Bảng tin cộng đồng.
 */
public interface NotificationService {

    /** Các bình luận mới trên bài viết của tôi / trả lời bình luận của tôi (mới nhất trước). */
    NotificationItem.Page list(Long userId, int size);

    /** Đánh dấu đã xem tất cả thông báo. */
    void markAllSeen(Long userId);
}
